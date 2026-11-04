package controllers

import javax.inject.{Inject, Singleton}

import models.{GruntView, SessionRecord, User, UserView}
import play.api.{Configuration, Logging}
import play.api.libs.json.*
import play.api.mvc.*
import repositories.DatabaseHealth
import services.{AccountService, AuthThrottle, GruntService, ImageService, SessionService, SignUp, VideoService}

import scala.concurrent.{ExecutionContext, Future}

/** The JSON API the web client talks to. Everything lives under /api (see conf/routes). */
@Singleton
class ApiController @Inject() (
    cc: ControllerComponents,
    auth: Auth,
    accounts: AccountService,
    gruntService: GruntService,
    images: ImageService,
    videos: VideoService,
    throttle: AuthThrottle,
    sessions: SessionService,
    health: DatabaseHealth,
    config: Configuration
)(using ec: ExecutionContext) extends AbstractController(cc) with Logging {
  import auth.{SessionKey, SignedIn, Viewer}

  private val allowRegistration = config.get[Boolean]("openwalrus.allowRegistration")

  private given Reads[SignUp] = Json.reads[SignUp]
  private case class SignIn(handle: String, password: String)
  private given Reads[SignIn] = Json.reads[SignIn]
  private case class PostGrunt(message: String, friendsOnly: Option[Boolean])
  private given Reads[PostGrunt] = Json.reads[PostGrunt]
  private case class ImageRef(imageId: String)
  private given Reads[ImageRef] = Json.reads[ImageRef]
  private case class PasswordChange(current: String, next: String)
  private given Reads[PasswordChange] = Json.reads[PasswordChange]
  private case class Reply(message: String)
  private given Reads[Reply] = Json.reads[Reply]
  private case class GruntChange(friendsOnly: Boolean)
  private given Reads[GruntChange] = Json.reads[GruntChange]
  private case class Viewed(ids: Seq[String])
  private given Reads[Viewed] = Json.reads[Viewed]

  private def error(message: String) = Json.obj("error" -> message)

  /** Parses the JSON body as T, or answers 400 with what was wrong. */
  private def withJson[T: Reads](request: Request[AnyContent])(f: T => Future[Result]): Future[Result] =
    request.body.asJson.map(_.validate[T]) match {
      case Some(JsSuccess(value, _)) => f(value)
      case _ => Future.successful(BadRequest(error("The request was not understood.")))
    }

  private def withUser(handle: String)(f: User => Future[Result]): Future[Result] =
    accounts.findByHandle(handle).flatMap {
      case Some(user) => f(user)
      case None => Future.successful(NotFound(error(s"There is no @$handle here.")))
    }

  private val TooManyTries = "Too many wrong passwords. Try again in a few minutes."

  /**
   * Signs the browser in as the user with a new session (any it had is ended), and answers
   * with the user. A fresh token at every sign-in means no one can plant one beforehand.
   */
  private def signInAs(user: User, request: ViewerRequest[?], status: Status): Future[Result] =
    for {
      _ <- endViewerSession(request)
      token <- sessions.start(user, request.headers.get(USER_AGENT).getOrElse(""), request.remoteAddress)
      view <- accounts.view(user, Some(user.id))
    } yield status(Json.toJson(view)).withNewSession.withSession(SessionKey -> token)

  // ---- health and session ----

  def healthCheck: Action[AnyContent] = Action.async {
    health.ping().map(_ => Ok(Json.obj("status" -> "ok"))).recover { case e =>
      // The detail goes to the log only: this endpoint is public.
      logger.warn(s"Health check: database unreachable: ${e.getMessage}")
      ServiceUnavailable(Json.obj("status" -> "database unreachable"))
    }
  }

  def session: Action[AnyContent] = Viewer.async { request =>
    val me = request.viewer.fold(Future.successful(JsNull: JsValue))(u => accounts.view(u, Some(u.id)).map(Json.toJson(_)))
    me.map(user => Ok(Json.obj("user" -> user, "allowRegistration" -> allowRegistration)))
  }

  private def endViewerSession(request: ViewerRequest[?]): Future[Unit] =
    request.viewer.zip(request.viewerSession).fold(Future.unit)((u, s) => sessions.end(u, s.id).map(_ => ()))

  def signIn: Action[AnyContent] = Viewer.async { request =>
    withJson[SignIn](request) { form =>
      val address = request.remoteAddress
      if (throttle.signInBlocked(form.handle, address)) Future.successful(TooManyRequests(error(TooManyTries)))
      else accounts.signIn(form.handle, form.password).flatMap {
        case Some(user) =>
          throttle.signInSucceeded(form.handle)
          signInAs(user, request, Ok)
        case None =>
          throttle.signInFailed(form.handle, address)
          Future.successful(Unauthorized(error("That handle and password don't match.")))
      }
    }
  }

  /** Ends the session for good, not just in this browser. */
  def signOut: Action[AnyContent] = Viewer.async { request =>
    endViewerSession(request).map(_ => NoContent.withNewSession)
  }

  def signUp: Action[AnyContent] = Viewer.async { request =>
    val address = request.remoteAddress
    if (!allowRegistration) Future.successful(Forbidden(error("Sign-ups are closed here.")))
    else if (throttle.signUpBlocked(address)) Future.successful(TooManyRequests(error("Too many new accounts from here. Try again later.")))
    else withJson[SignUp](request) { form =>
      accounts.signUp(form).flatMap {
        case Left(problem) => Future.successful(BadRequest(error(problem)))
        case Right(user) =>
          throttle.signedUp(address)
          signInAs(user, request, Created)
      }
    }
  }

  // ---- your account's security ----

  /** Wrong current passwords count against the handle like wrong sign-ins, so a borrowed session can't guess it. */
  def changePassword: Action[AnyContent] = SignedIn.async { request =>
    withJson[PasswordChange](request) { form =>
      val (user, address) = (request.user, request.remoteAddress)
      if (throttle.signInBlocked(user.handle, address)) Future.successful(TooManyRequests(error(TooManyTries)))
      else accounts.changePassword(user, form.current, form.next).flatMap {
        case Left(problem) =>
          if (problem == AccountService.WrongCurrentPassword) throttle.signInFailed(user.handle, address)
          Future.successful(BadRequest(error(problem)))
        case Right(()) =>
          // Anyone else signed in as you is signed out.
          sessions.endAllBut(user, Some(request.currentSession.id)).map(_ => NoContent)
      }
    }
  }

  /** Where you're signed in. */
  def mySessions: Action[AnyContent] = SignedIn.async { request =>
    sessions.list(request.user).map(list => Ok(Json.toJson(list.map(sessionJson(_, request.currentSession.id)))))
  }

  /** Signs out one of your sessions; this one included, which also clears the cookie. */
  def endSession(id: String): Action[AnyContent] = SignedIn.async { request =>
    sessions.end(request.user, id).map {
      case false => NotFound(error("That session has ended already."))
      case true if id == request.currentSession.id => NoContent.withNewSession
      case true => NoContent
    }
  }

  /** Signs out everywhere but here. */
  def endOtherSessions: Action[AnyContent] = SignedIn.async { request =>
    sessions.endAllBut(request.user, Some(request.currentSession.id)).map(_ => NoContent)
  }

  private def sessionJson(s: SessionRecord, currentId: String) = Json.obj(
    "id" -> s.id,
    "current" -> (s.id == currentId),
    "createdAt" -> s.createdAt,
    "lastSeenAt" -> s.lastSeenAt,
    "userAgent" -> s.userAgent,
    "address" -> s.address
  )

  /** For the sign-up form, as you type: {available: true}, or false with the reason. */
  def handleAvailability(handle: String): Action[AnyContent] = Action.async {
    accounts.handleProblem(handle).map {
      case None => Ok(Json.obj("available" -> true))
      case Some(problem) => Ok(Json.obj("available" -> false, "reason" -> problem))
    }
  }

  // ---- people ----

  def users: Action[AnyContent] = Viewer.async { request =>
    accounts.everyone(request.viewerId).map(list => Ok(Json.toJson(list)))
  }

  def user(handle: String): Action[AnyContent] = Viewer.async { request =>
    withUser(handle)(u => accounts.view(u, request.viewerId).map(v => Ok(Json.toJson(v))))
  }

  def userGrunts(handle: String, before: Option[Long]): Action[AnyContent] = Viewer.async { request =>
    withUser(handle)(u => gruntService.profileFeed(u, before, request.viewer).map(list => Ok(Json.toJson(list))))
  }

  def followers(handle: String): Action[AnyContent] = Viewer.async { request =>
    withUser(handle)(u => accounts.followers(u, request.viewerId).map(list => Ok(Json.toJson(list))))
  }

  def following(handle: String): Action[AnyContent] = Viewer.async { request =>
    withUser(handle)(u => accounts.following(u, request.viewerId).map(list => Ok(Json.toJson(list))))
  }

  def follow(handle: String): Action[AnyContent] = SignedIn.async { request =>
    withUser(handle)(target => accounts.setFollowing(request.user, target, follow = true).map(v => Ok(Json.toJson(v))))
  }

  def unfollow(handle: String): Action[AnyContent] = SignedIn.async { request =>
    withUser(handle)(target => accounts.setFollowing(request.user, target, follow = false).map(v => Ok(Json.toJson(v))))
  }

  def setProfileImage: Action[AnyContent] = SignedIn.async { request =>
    withJson[ImageRef](request)(ref => accounts.setProfileImage(request.user, ref.imageId).map(_ => NoContent))
  }

  def setBannerImage: Action[AnyContent] = SignedIn.async { request =>
    withJson[ImageRef](request)(ref => accounts.setBannerImage(request.user, ref.imageId).map(_ => NoContent))
  }

  // ---- grunts ----

  def feed(before: Option[Long]): Action[AnyContent] = SignedIn.async { request =>
    gruntService.homeFeed(request.user, before).map(list => Ok(Json.toJson(list)))
  }

  def postGrunt: Action[AnyContent] = SignedIn.async { request =>
    withJson[PostGrunt](request) { form =>
      gruntService.post(request.user, form.message, form.friendsOnly.getOrElse(false)).map {
        case Left(problem) => BadRequest(error(problem))
        case Right(view) => Created(Json.toJson(view))
      }
    }
  }

  def grunt(id: String): Action[AnyContent] = Viewer.async { request =>
    gruntService.one(id, request.viewer).map {
      case Some(view) => Ok(Json.toJson(view))
      case None => NotFound(error(GruntGone))
    }
  }

  def gruntStats(id: String): Action[AnyContent] = Viewer.async { request =>
    gruntService.stats(id, request.viewer).map {
      case Some(stats) => Ok(Json.toJson(stats))
      case None => NotFound(error(GruntGone))
    }
  }

  def replies(id: String): Action[AnyContent] = Viewer.async { request =>
    gruntService.replies(id, request.viewer).map {
      case Some(list) => Ok(Json.toJson(list))
      case None => NotFound(error(GruntGone))
    }
  }

  def postReply(id: String): Action[AnyContent] = SignedIn.async { request =>
    withJson[Reply](request)(form => gruntService.reply(request.user, id, form.message).map(answer(_, Created)))
  }

  def regrunt(id: String): Action[AnyContent] = SignedIn.async { request =>
    gruntService.setRegrunt(request.user, id, on = true).map(answer(_, Ok))
  }

  def unregrunt(id: String): Action[AnyContent] = SignedIn.async { request =>
    gruntService.setRegrunt(request.user, id, on = false).map(answer(_, Ok))
  }

  /** For now the one thing to change after posting: who can see it. */
  def changeGrunt(id: String): Action[AnyContent] = SignedIn.async { request =>
    withJson[GruntChange](request)(form => gruntService.setFriendsOnly(request.user, id, form.friendsOnly).map(answer(_, Ok)))
  }

  private val GruntGone = "That grunt is gone, or isn't shared with you."

  private def answer(result: Either[GruntService.Problem, GruntView], status: Status): Result = result match {
    case Right(view) => status(Json.toJson(view))
    case Left(GruntService.Problem.NotFound) => NotFound(error(GruntGone))
    case Left(GruntService.Problem.NotAllowed(message)) => Forbidden(error(message))
    case Left(GruntService.Problem.Invalid(message)) => BadRequest(error(message))
  }

  def hot: Action[AnyContent] = Viewer.async { request =>
    gruntService.hot(request.viewer).map(list => Ok(Json.toJson(list)))
  }

  /** The web client reports the grunts that were on screen long enough to count as seen. */
  def recordViews: Action[AnyContent] = SignedIn.async { request =>
    withJson[Viewed](request)(v => gruntService.recordViews(request.user, v.ids).map(_ => NoContent))
  }

  def deleteGrunt(id: String): Action[AnyContent] = SignedIn.async { request =>
    gruntService.delete(request.user, id).map {
      case GruntService.Deletion.Deleted => NoContent
      case GruntService.Deletion.NotFound => NotFound(error("That grunt is gone already."))
      case GruntService.Deletion.NotYours => Forbidden(error("You can only delete your own grunts."))
    }
  }

  // ---- images ----

  def uploadImage: Action[play.api.mvc.MultipartFormData[play.api.libs.Files.TemporaryFile]] =
    SignedIn(parse.multipartFormData(maxLength = ImageUploadLimit)).async { request =>
      request.body.file("picture") match {
        case None => Future.successful(BadRequest(error("Choose a picture to upload.")))
        case Some(file) =>
          images.store(file.ref.path, file.filename).map {
            case Left(problem) => BadRequest(error(problem))
            case Right(id) => Created(Json.obj("id" -> id))
          }
      }
    }

  def image(id: String): Action[AnyContent] = serveImage(id, thumb = false)

  def imageThumb(id: String): Action[AnyContent] = serveImage(id, thumb = true)

  /** Images never change once stored, so the id is a perfectly good ETag. */
  private def serveImage(id: String, thumb: Boolean): Action[AnyContent] = Action.async { request =>
    val etag = "\"" + id + (if (thumb) "-t" else "") + "\""
    if (request.headers.get(IF_NONE_MATCH).contains(etag)) Future.successful(NotModified)
    else images.find(id, thumb).map {
      case Some(img) => Ok(img.bytes).as(img.contentType)
        .withHeaders(ETAG -> etag, CACHE_CONTROL -> "public, max-age=31536000, immutable", "X-Content-Type-Options" -> "nosniff")
      case None => NotFound
    }
  }

  private val ImageUploadLimit = 10L * 1024 * 1024

  // ---- videos ----

  def uploadVideo: Action[play.api.mvc.MultipartFormData[play.api.libs.Files.TemporaryFile]] =
    SignedIn(parse.multipartFormData(maxLength = VideoService.MaxBytes + 1024 * 1024)).async { request =>
      request.body.file("video") match {
        case None => Future.successful(BadRequest(error("Choose a video to upload.")))
        case Some(file) if file.fileSize > VideoService.MaxBytes => Future.successful(EntityTooLarge(error("Videos can be 50 MB at most.")))
        case Some(file) =>
          videos.store(file.ref.path, file.filename).map {
            case Left(problem) => BadRequest(error(problem))
            case Right(id) => Created(Json.obj("id" -> id))
          }
      }
    }

  /** Answers byte-range requests, which browsers use to start playing early and to seek. */
  def video(id: String): Action[AnyContent] = Action.async { request =>
    videos.open(id).map {
      case None => NotFound
      case Some(v) =>
        RangeResult.ofSource(Some(v.length), v.readFrom, request.headers.get(RANGE), None, Some(v.contentType))
          .withHeaders(CACHE_CONTROL -> "public, max-age=31536000, immutable", "X-Content-Type-Options" -> "nosniff")
    }
  }
}
