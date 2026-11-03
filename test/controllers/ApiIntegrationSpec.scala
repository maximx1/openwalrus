package controllers

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.{Result, Session, SessionCookieBaker}
import play.api.test.FakeRequest
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

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> s"${mongoUri.get.stripSuffix("/")}/$dbName")
    .build()

  override def afterAll(): Unit = if (mongoUri.isDefined) {
    import org.mongodb.scala.SingleObservableFuture
    await(app.injector.instanceOf[repositories.mongo.MongoDatabaseProvider].database.drop().toFuture())
    await(app.stop())
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

  private def signUp(handle: String): Session = {
    val res = call(POST, "/api/users", Some(Json.obj("fullName" -> s"$handle Walrus", "handle" -> handle, "password" -> "password123")))
    status(res) mustBe CREATED
    session(res)
  }

  "The API" should {
    "sign up, post, show a profile, and follow" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")

      status(call(GET, "/api/health")) mustBe OK

      val alice = signUp("alice")
      val bob = signUp("bob")

      // the same handle in another case is taken
      status(call(POST, "/api/users", Some(Json.obj("fullName" -> "x", "handle" -> "ALICE", "password" -> "password123")))) mustBe BAD_REQUEST

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

      // sign in with the password; wrong password refused
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "Alice", "password" -> "password123")))) mustBe OK
      status(call(POST, "/api/session", Some(Json.obj("handle" -> "alice", "password" -> "wrong-one")))) mustBe UNAUTHORIZED
    }

    "refuse a form posted from another site" in {
      assume(mongoUri.isDefined, "TEST_MONGO_URI is not set")
      val alice = call(POST, "/api/session", Some(Json.obj("handle" -> "alice", "password" -> "password123")))
      val forged = signedIn(FakeRequest(POST, "/api/grunts"), session(alice)).withTextBody("""{"message": "forged"}""")
      // A plain HTML form can only send form, multipart or text bodies. None of them is JSON,
      // so nothing is posted, whatever the cookie.
      status(route(app, forged).get) must not be CREATED
      contentAsJson(call(GET, "/api/users/alice/grunts")).as[JsArray].value.map(g => (g \ "html").as[String]).mkString must not include "forged"
    }
  }
}
