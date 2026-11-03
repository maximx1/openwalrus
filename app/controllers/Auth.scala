package controllers

import javax.inject.{Inject, Singleton}

import models.User
import play.api.libs.json.Json
import play.api.mvc.*
import services.AccountService

import scala.concurrent.{ExecutionContext, Future}

/** A request with whoever is signed in, if anyone. The session cookie holds only their id. */
class ViewerRequest[A](val viewer: Option[User], request: Request[A]) extends WrappedRequest[A](request) {
  def viewerId: Option[String] = viewer.map(_.id)
}

class SignedInRequest[A](val user: User, request: Request[A]) extends ViewerRequest[A](Some(user), request)

@Singleton
class Auth @Inject() (parser: BodyParsers.Default, accounts: AccountService)(using ec: ExecutionContext) {
  val SessionKey = "userId"

  /** Looks up the signed-in user, if any. */
  object Viewer extends ActionBuilder[ViewerRequest, AnyContent] {
    def parser: BodyParser[AnyContent] = Auth.this.parser
    protected def executionContext: ExecutionContext = ec
    def invokeBlock[A](request: Request[A], block: ViewerRequest[A] => Future[Result]): Future[Result] =
      request.session.get(SessionKey).fold(Future.successful(Option.empty[User]))(accounts.findById)
        .flatMap(user => block(new ViewerRequest(user, request)))
  }

  /** Refuses with 401 unless someone is signed in. */
  object SignedIn extends ActionBuilder[SignedInRequest, AnyContent] {
    def parser: BodyParser[AnyContent] = Auth.this.parser
    protected def executionContext: ExecutionContext = ec
    def invokeBlock[A](request: Request[A], block: SignedInRequest[A] => Future[Result]): Future[Result] =
      request.session.get(SessionKey).fold(Future.successful(Option.empty[User]))(accounts.findById).flatMap {
        case Some(user) => block(new SignedInRequest(user, request))
        case None => Future.successful(Results.Unauthorized(Json.obj("error" -> "Sign in first.")).withNewSession)
      }
  }
}
