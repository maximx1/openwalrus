package controllers

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.{Result, Session, SessionCookieBaker}
import play.api.test.{FakeRequest, TestServer}
import play.api.test.Helpers.*

import scala.concurrent.Future

/**
 * The API end to end against a real MongoDB. Runs only when TEST_MONGO_URI is set, for
 * example: docker run -d -p 127.0.0.1:27027:27017 mongo:8, then
 * TEST_MONGO_URI=mongodb://localhost:27027 sbt test. Each run uses a fresh database.
 */
class ApiIntegrationSpec extends PlaySpec with BeforeAndAfterAll {

  private val mongoUri = sys.env.get("TEST_MONGO_URI")
  private val dbName = s"openwalrus_test_${System.currentTimeMillis()}"

  private val Password = "tusks-and-whiskers"

  // Every test request comes from the same address, so lift the per-address sign-up limit.
  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> s"${mongoUri.get.stripSuffix("/")}/$dbName", "openwalrus.throttle.signUpsPerAddress" -> 1000)
    .build()

  // The last test stops the app along with its server, so clean up with a client of our own.
  override def afterAll(): Unit = mongoUri.foreach { uri =>
    import org.mongodb.scala.{MongoClient, SingleObservableFuture}
    val client = MongoClient(uri)
    try await(client.getDatabase(dbName).drop().toFuture())
    finally client.close()
  }

  private def call(method: String, path: String, body: Option[JsValue] = None, session: Option[Session] = None): Future[Result] = {
    val base = FakeRequest(method, path).withHeaders("X-Requested-With" -> "test")
    val withCookie = session.fold(base)(signedIn(base, _))
    body match {
      case Some(json) => route(app, withCookie.withJsonBody(json)).get
      case None => route(app, withCookie).get
    }
  }

  /**
   * FakeRequest keeps the session apart from its headers, so set both: the session for the
   * app, and the cookie a browser would send for the CSRF filter to see.
   */
  private def signedIn[A](req: FakeRequest[A], s: Session): FakeRequest[A] =
    req.withSession(s.data.toSeq*).withCookies(app.injector.instanceOf[SessionCookieBaker].encodeAsCookie(s))

  private def availability(h: String) = contentAsJson(call(GET, s"/api/handles/$h"))

  private def signUp(handle: String): Session = {
    val res = call(POST, "/api/users", Some(Json.obj("fullName" -> s"$handle Walrus", "handle" -> handle, "password" -> Password)))
    status(res) mustBe CREATED
    session(res)
  }

  "The API" should {
    "sign up, post, show a profile, and follow" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")

      status(call(GET, "/api/health")) mustBe OK

      val alice = signUp("alice")
      val bob = signUp("bob")

      // the sign-up form's live check
      (availability("ALICE") \ "available").as[Boolean] mustBe false
      (availability("ALICE") \ "reason").as[String] must include("taken")
      (availability("carol") \ "available").as[Boolean] mustBe true
      (availability("team") \ "available").as[Boolean] mustBe false
      (availability("no%20spaces") \ "reason").as[String] must include("letters, numbers or underscores")

      // the same handle in another case is taken
      status(call(POST, "/api/users", Some(Json.obj("fullName" -> "x", "handle" -> "ALICE", "password" -> Password)))) mustBe BAD_REQUEST

      val posted = call(POST, "/api/grunts", Some(Json.obj("message" -> "hello **world**, @bob")), Some(alice))
      status(posted) mustBe CREATED
      (contentAsJson(posted) \ "html").as[String] must include("<strong>world</strong>")

      // not signed in: no posting
      status(call(POST, "/api/grunts", Some(Json.obj("message" -> "nope")))) mustBe UNAUTHORIZED

      // alice's profile has the grunt; bob's has it too, as a mention
      contentAsJson(call(GET, "/api/users/alice/grunts")).as[JsArray].value must have size 1
      contentAsJson(call(GET, "/api/users/bob/grunts")).as[JsArray].value must have size 1
      (contentAsJson(call(GET, "/api/users/alice")) \ "gruntCount").as[Int] mustBe 1

      // bob follows alice
      val followed = call(PUT, "/api/users/alice/follow", None, Some(bob))
      status(followed) mustBe OK
      (contentAsJson(followed) \ "followedByMe").as[Boolean] mustBe true
      (contentAsJson(followed) \ "followerCount").as[Int] mustBe 1
      contentAsJson(call(GET, "/api/users/bob/following")).as[JsArray].value.map(u => (u \ "handle").as[String]) mustBe Seq("alice")

      // bob's home feed has alice's grunt
      contentAsJson(call(GET, "/api/feed", None, Some(bob))).as[JsArray].value must have size 1

      // only the author can delete it
      val gruntId = (contentAsJson(posted) \ "id").as[String]
      status(call(DELETE, s"/api/grunts/$gruntId", None, Some(bob))) mustBe FORBIDDEN
      status(call(DELETE, s"/api/grunts/$gruntId", None, Some(alice))) mustBe NO_CONTENT
      contentAsJson(call(GET, "/api/users/alice/grunts")).as[JsArray].value mustBe empty
      status(call(POST, "/api/grunts", Some(Json.obj("message" -> "back again")), Some(alice))) mustBe CREATED

      // sign in with the password; wrong password refused
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "Alice", "password" -> Password)))) mustBe OK
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "alice", "password" -> "wrong-one")))) mustBe UNAUTHORIZED
    }

    "show grunts the 2015 version recorded as mentions on the mentioned user's profile" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      import org.bson.Document
      import org.bson.types.ObjectId
      import org.mongodb.scala.SingleObservableFuture
      import org.mongodb.scala.model.Filters
      import org.mongodb.scala.model.Updates.push
      val mongo = app.injector.instanceOf[repositories.mongo.MongoDatabaseProvider]
      val alice = await(mongo.users.find(Filters.equal("handle", "alice")).head())
      val oldGrunt = new ObjectId()
      // As the 2015 version wrote it: no "mentions"; the id pushed onto bob's "grunts" instead.
      await(mongo.grunts.insertOne(new Document("_id", oldGrunt).append("userId", alice.getObjectId("_id"))
        .append("message", "an old @bob grunt").append("timestamp", 1000L)).toFuture())
      await(mongo.users.updateOne(Filters.equal("handle", "bob"), push("grunts", oldGrunt)).toFuture())
      contentAsJson(call(GET, "/api/users/bob/grunts")).as[JsArray].value.map(g => (g \ "id").as[String]) must contain(oldGrunt.toHexString)
    }

    "refuse sign-in for a while after too many wrong passwords" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      val wrong = Some(Json.obj("handle" -> "bob", "password" -> "not-it-at-all"))
      (1 to services.AuthThrottle.MaxFailuresPerHandle).foreach(_ => status(call(POST, "/api/session", wrong)) mustBe UNAUTHORIZED)
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "bob", "password" -> Password)))) mustBe TOO_MANY_REQUESTS
    }

    "keep friends-only grunts to the author's friends, count views once per person, and rank what's hot" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      val dave = signUp("dave")
      val erin = signUp("erin")
      val frank = signUp("frank")
      def post(message: String, friendsOnly: Boolean) =
        (contentAsJson(call(POST, "/api/grunts", Some(Json.obj("message" -> message, "friendsOnly" -> friendsOnly)), Some(dave))) \ "id").as[String]
      val open = post("for everyone", friendsOnly = false)
      val closed = post("for friends", friendsOnly = true)
      // dave's friends are the people he follows: erin, not frank
      status(call(PUT, "/api/users/erin/follow", None, Some(dave))) mustBe OK
      status(call(PUT, "/api/users/dave/follow", None, Some(frank))) mustBe OK

      def ids(res: Future[Result]) = contentAsJson(res).as[JsArray].value.map(g => (g \ "id").as[String]).toSeq
      ids(call(GET, "/api/users/dave/grunts", None, Some(dave))) must contain allOf (open, closed)
      ids(call(GET, "/api/users/dave/grunts", None, Some(erin))) must contain allOf (open, closed)
      ids(call(GET, "/api/users/dave/grunts", None, Some(frank))) mustBe Seq(open)
      ids(call(GET, "/api/users/dave/grunts")) mustBe Seq(open)
      ids(call(GET, "/api/feed", None, Some(frank))) mustBe Seq(open)

      def view(who: Session, gruntIds: String*) =
        status(call(POST, "/api/views", Some(Json.obj("ids" -> gruntIds)), Some(who))) mustBe NO_CONTENT
      def views = contentAsJson(call(GET, "/api/users/dave/grunts", None, Some(dave))).as[JsArray].value
        .map(g => (g \ "id").as[String] -> (g \ "views").as[Int]).toMap
      view(frank, open, closed) // frank can't see the friends-only one, so it isn't counted
      view(frank, open)         // a second look isn't another view
      view(dave, open, closed)  // nor is the author's own
      views mustBe Map(open -> 1, closed -> 0)
      view(erin, closed)
      views mustBe Map(open -> 1, closed -> 1)
      status(call(POST, "/api/views", Some(Json.obj("ids" -> Seq(open))))) mustBe UNAUTHORIZED

      val unseen = post("nobody has looked", friendsOnly = false)
      ids(call(GET, "/api/hot")) mustBe Seq(open)
      ids(call(GET, "/api/hot", None, Some(erin))) must contain allOf (open, closed)
      ids(call(GET, "/api/hot", None, Some(erin))) must not contain unseen
      (availability("hot") \ "available").as[Boolean] mustBe false
    }

    "keep sessions on the server: sign-out, password changes and the session list end them" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      def signIn(handle: String, password: String) = call(POST, "/api/session", Some(Json.obj("handle" -> handle, "password" -> password)))
      def who(s: Session) = (contentAsJson(call(GET, "/api/session", None, Some(s))) \ "user" \ "handle").asOpt[String]
      def sessionsOf(s: Session) = contentAsJson(call(GET, "/api/me/sessions", None, Some(s))).as[JsArray].value

      val laptop = signUp("gina")
      val phone = session(signIn("gina", Password))
      val tablet = session(signIn("gina", Password))
      Seq(laptop, phone, tablet).map(who) mustBe Seq(Some("gina"), Some("gina"), Some("gina"))
      laptop.get("sid") must not equal phone.get("sid")
      sessionsOf(laptop) must have size 3
      sessionsOf(laptop).count(s => (s \ "current").as[Boolean]) mustBe 1

      // Signing out ends that session on the server: a copy of the cookie is useless.
      status(call(DELETE, "/api/session", None, Some(tablet))) mustBe NO_CONTENT
      who(tablet) mustBe None
      status(call(POST, "/api/grunts", Some(Json.obj("message" -> "still me?")), Some(tablet))) mustBe UNAUTHORIZED

      // Ending another session from the list.
      val phoneId = sessionsOf(laptop).find(s => !(s \ "current").as[Boolean]).map(s => (s \ "id").as[String]).get
      status(call(DELETE, s"/api/me/sessions/$phoneId", None, Some(laptop))) mustBe NO_CONTENT
      who(phone) mustBe None
      status(call(DELETE, s"/api/me/sessions/$phoneId", None, Some(laptop))) mustBe NOT_FOUND
      // Someone else's session can't be ended.
      val other = signUp("hank")
      val hanksId = (sessionsOf(other).head \ "id").as[String]
      status(call(DELETE, s"/api/me/sessions/$hanksId", None, Some(laptop))) mustBe NOT_FOUND
      who(other) mustBe Some("hank")

      // A new password needs the current one, and signs out everywhere else.
      val desk = session(signIn("gina", Password))
      def change(s: Session, current: String, next: String) =
        call(PUT, "/api/me/password", Some(Json.obj("current" -> current, "next" -> next)), Some(s))
      status(change(laptop, "not-my-password", "a-whole-new-one")) mustBe BAD_REQUEST
      (contentAsJson(change(laptop, Password, "password123")) \ "error").as[String] must include("easy to guess")
      status(change(laptop, Password, "a-whole-new-one")) mustBe NO_CONTENT
      who(laptop) mustBe Some("gina")
      who(desk) mustBe None
      status(signIn("gina", Password)) mustBe UNAUTHORIZED
      status(signIn("gina", "a-whole-new-one")) mustBe OK

      // The 2015 cookie, which held the user's id, signs no one in now.
      val ginaId = (contentAsJson(call(GET, "/api/session", None, Some(laptop))) \ "user" \ "id").as[String]
      who(Session(Map("userId" -> ginaId))) mustBe None
      who(Session(Map("sid" -> ginaId))) mustBe None
    }

    "sign in an account with a 2015 bcrypt hash, and replace the hash with Argon2id" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      import org.bson.Document
      import org.mindrot.jbcrypt.BCrypt
      import org.mongodb.scala.SingleObservableFuture
      import org.mongodb.scala.model.Filters
      val mongo = app.injector.instanceOf[repositories.mongo.MongoDatabaseProvider]
      await(mongo.users.insertOne(new Document("handle", "oldtimer").append("fullName", "Old Timer")
        .append("password", BCrypt.hashpw("short1", BCrypt.gensalt(4))).append("creationDate", 1000L)).toFuture())
      // The password predates today's rules; it still works.
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "oldtimer", "password" -> "short1")))) mustBe OK
      await(mongo.users.find(Filters.equal("handle", "oldtimer")).head()).getString("password") must startWith("$argon2id$")
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "oldtimer", "password" -> "short1")))) mustBe OK
    }

    "store a video and serve it in byte ranges, from any chunk" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      given org.apache.pekko.stream.Materializer = app.materializer // to read a streamed body
      // An MP4 header, then enough bytes to span several GridFS chunks (255 KB each).
      val body = "\u0000\u0000\u0000\u0018ftypisom".getBytes("ISO-8859-1") ++ Array.tabulate[Byte](700 * 1024)(i => (i % 251).toByte)
      val file = java.nio.file.Files.createTempFile("video", ".mp4")
      java.nio.file.Files.write(file, body)
      val id = await(app.injector.instanceOf[services.VideoService].store(file, "clip.mp4")).toOption.get

      val whole = call(GET, s"/api/videos/$id")
      status(whole) mustBe OK
      contentType(whole) mustBe Some("video/mp4")
      contentAsBytes(whole).toArray mustBe body

      val from = 300 * 1024 + 7 // inside the second chunk
      val part = route(app, FakeRequest(GET, s"/api/videos/$id").withHeaders(RANGE -> s"bytes=$from-${from + 99}")).get
      status(part) mustBe PARTIAL_CONTENT
      header(CONTENT_RANGE, part) mustBe Some(s"bytes $from-${from + 99}/${body.length}")
      contentAsBytes(part).toArray mustBe body.slice(from, from + 100)

      status(call(GET, "/api/videos/6aeb3377726319dc659be286")) mustBe NOT_FOUND
    }

    "over real HTTP, accept the web client's requests and refuse ones forged by another site" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      import java.net.URI
      import java.net.http.{HttpClient, HttpRequest, HttpResponse}
      val port = 19123
      val server = TestServer(port, app)
      server.start()
      try {
        val http = HttpClient.newHttpClient()
        def post(path: String, body: String, contentType: String, cookie: Option[String], fromClient: Boolean) = {
          val b = HttpRequest.newBuilder(URI.create(s"http://localhost:$port$path"))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
          cookie.foreach(c => b.header("Cookie", c))
          if (fromClient) b.header("X-Requested-With", "openwalrus")
          http.send(b.build(), HttpResponse.BodyHandlers.ofString())
        }
        val signIn = post("/api/session", s"""{"handle":"alice","password":"$Password"}""", "application/json", None, fromClient = true)
        signIn.statusCode mustBe OK
        val cookie = signIn.headers.firstValue("Set-Cookie").get.takeWhile(_ != ';')

        post("/api/grunts", """{"message":"from the client"}""", "application/json", Some(cookie), fromClient = true).statusCode mustBe CREATED
        post("/api/grunts", """{"message":"forged"}""", "text/plain", Some(cookie), fromClient = false).statusCode mustBe FORBIDDEN
        post("/api/grunts", "message=forged", "application/x-www-form-urlencoded", Some(cookie), fromClient = false).statusCode mustBe FORBIDDEN
      } finally server.stop()
    }
  }
}
