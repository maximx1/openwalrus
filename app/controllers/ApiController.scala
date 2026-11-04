package controllers

import javax.inject.{Inject, Singleton}

import models.{GruntView, User, UserView}
import play.api.{Configuration, Logging}
import play.api.libs.json.*
import play.api.mvc.*
import repositories.DatabaseHealth
import services.{AccountService, GruntService, ImageService, LoginThrottle, SignUp}

import scala.concurrent.{ExecutionContext, Future}

/** The JSON API the web client talks to. Everything lives under /api (see conf/routes). */
@Singleton
class ApiController @Inject() (
    cc: ControllerComponents,
    auth: Auth,
    accounts: AccountService,
    gruntService: GruntService,
    images: ImageService,
    throttle: LoginThrottle,
    health: DatabaseHealth,
    config: Configuration
)(using ec: ExecutionContext) extends AbstractController(cc) with Logging {
  import auth.{SessionKey, SignedIn, Viewer}

  private val allowRegistration = config.get[Boolean]("openwalrus.allowRegistration")

  private given Reads[SignUp] = Json.reads[SignUp]
  private case class SignIn(handle: String, password: String)
  private given Reads[SignIn] = Json.reads[SignIn]
  private case class PostGrunt(message: String)
  private given Reads[PostGrunt] = Json.reads[PostGrunt]
  private case class ImageRef(imageId: String)
  private given Reads[ImageRef] = Json.reads[ImageRef]

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

  private def signedInAs(user: User, result: Result): Result =
    result.withSession(SessionKey -> user.id)

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

  def signIn: Action[AnyContent] = Action.async { request =>
    withJson[SignIn](request) { form =>
      if (throttle.blocked(form.handle))
        Future.successful(TooManyRequests(error("Too many wrong passwords for that handle. Try again in a few minutes.")))
      else accounts.signIn(form.handle, form.password).flatMap {
        case Some(user) =>
          throttle.succeeded(form.handle)
          accounts.view(user, Some(user.id)).map(v => signedInAs(user, Ok(Json.toJson(v))))
        case None =>
          throttle.failed(form.handle)
          Future.successful(Unauthorized(error("That handle and password don't match.")))
      }
    }
  }

  def signOut: Action[AnyContent] = Action { NoContent.withNewSession }

  def signUp: Action[AnyContent] = Action.async { request =>
    if (!allowRegistration) Future.successful(Forbidden(error("Sign-ups are closed here.")))
    else withJson[SignUp](request) { form =>
      accounts.signUp(form).flatMap {
        case Left(problem) => Future.successful(BadRequest(error(problem)))
        case Right(user) => accounts.view(user, Some(user.id)).map(v => signedInAs(user, Created(Json.toJson(v))))
      }
    }
  }

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

  def userGrunts(handle: String, before: Option[Long]): Action[AnyContent] = Action.async {
    withUser(handle)(u => gruntService.profileFeed(u, before).map(list => Ok(Json.toJson(list))))
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
      gruntService.post(request.user, form.message).map {
        case Left(problem) => BadRequest(error(problem))
        case Right(view) => Created(Json.toJson(view))
      }
    }
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
}
