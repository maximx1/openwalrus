package controllers

import javax.inject.{Inject, Singleton}

import models.{SessionRecord, User}
import play.api.libs.json.Json
import play.api.mvc.*
import services.SessionService

import scala.concurrent.{ExecutionContext, Future}

/** A request with whoever is signed in, if anyone, and the session they're signed in with. */
class ViewerRequest[A](val viewer: Option[User], val viewerSession: Option[SessionRecord], request: Request[A])
    extends WrappedRequest[A](request) {
  def viewerId: Option[String] = viewer.map(_.id)
}

class SignedInRequest[A](val user: User, val currentSession: SessionRecord, request: Request[A])
    extends ViewerRequest[A](Some(user), Some(currentSession), request)

@Singleton
class Auth @Inject() (parser: BodyParsers.Default, sessions: SessionService)(using ec: ExecutionContext) {
  /** The cookie's one entry: the session token (see SessionService). */
  val SessionKey = "sid"

  private def lookup(request: RequestHeader): Future[Option[(SessionRecord, User)]] =
    request.session.get(SessionKey).fold(Future.successful(None))(sessions.resolve(_))

  /** Looks up the signed-in user, if any. */
  object Viewer extends ActionBuilder[ViewerRequest, AnyContent] {
    def parser: BodyParser[AnyContent] = Auth.this.parser
    protected def executionContext: ExecutionContext = ec
    def invokeBlock[A](request: Request[A], block: ViewerRequest[A] => Future[Result]): Future[Result] =
      lookup(request).flatMap(found => block(new ViewerRequest(found.map(_._2), found.map(_._1), request)))
  }

  /** Refuses with 401 unless someone is signed in. */
  object SignedIn extends ActionBuilder[SignedInRequest, AnyContent] {
    def parser: BodyParser[AnyContent] = Auth.this.parser
    protected def executionContext: ExecutionContext = ec
    def invokeBlock[A](request: Request[A], block: SignedInRequest[A] => Future[Result]): Future[Result] =
      lookup(request).flatMap {
        case Some((session, user)) => block(new SignedInRequest(user, session, request))
        case None => Future.successful(Results.Unauthorized(Json.obj("error" -> "Sign in first.")).withNewSession)
      }
  }
}
