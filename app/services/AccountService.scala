package services

import javax.inject.{Inject, Singleton}

import models.{NewUser, User, UserView}
import org.mindrot.jbcrypt.BCrypt
import repositories.{GruntRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

case class SignUp(fullName: String, handle: String, password: String)

/** Signing up, signing in, following, and the profile data that goes with them. */
@Singleton
class AccountService @Inject() (users: UserRepository, grunts: GruntRepository)(using ec: ExecutionContext) {
  import AccountService.*

  /** Left is a message for the person signing up. */
  def signUp(form: SignUp): Future[Either[String, User]] = {
    val handle = form.handle.trim.stripPrefix("@")
    val fullName = form.fullName.trim
    validate(fullName, handle, form.password) match {
      case Some(problem) => Future.successful(Left(problem))
      case None =>
        users.findByHandle(handle).flatMap {
          case Some(_) => Future.successful(Left("That handle is taken."))
          case None =>
            val hash = BCrypt.hashpw(form.password, BCrypt.gensalt())
            users.create(NewUser(handle, fullName, hash, System.currentTimeMillis())).map(Right(_))
        }
    }
  }

  def signIn(handle: String, password: String): Future[Option[User]] =
    users.findByHandle(handle.trim.stripPrefix("@")).map(_.filter(u => checkPassword(password, u.passwordHash)))

  def findById(id: String): Future[Option[User]] = users.findById(id)

  def findByHandle(handle: String): Future[Option[User]] = users.findByHandle(handle)

  /** Everyone, for the Team page. */
  def everyone(viewerId: Option[String]): Future[Seq[UserView]] =
    users.all().flatMap(views(_, viewerId)).map(_.sortBy(_.handle.toLowerCase))

  def view(user: User, viewerId: Option[String]): Future[UserView] = views(Seq(user), viewerId).map(_.head)

  def followers(user: User, viewerId: Option[String]): Future[Seq[UserView]] =
    users.findByIds(user.followers).flatMap(views(_, viewerId))

  def following(user: User, viewerId: Option[String]): Future[Seq[UserView]] =
    users.findByIds(user.following).flatMap(views(_, viewerId))

  /** Follows or unfollows, and returns the target as it now stands. */
  def setFollowing(viewer: User, target: User, follow: Boolean): Future[UserView] = {
    val change =
      if (viewer.id == target.id) Future.unit
      else if (follow) users.follow(viewer.id, target.id)
      else users.unfollow(viewer.id, target.id)
    for {
      _ <- change
      updated <- users.findById(target.id)
      view <- view(updated.getOrElse(target), Some(viewer.id))
    } yield view
  }

  def setProfileImage(user: User, imageId: String): Future[Unit] = users.setProfileImage(user.id, imageId)

  def setBannerImage(user: User, imageId: String): Future[Unit] = users.setBannerImage(user.id, imageId)

  private def views(list: Seq[User], viewerId: Option[String]): Future[Seq[UserView]] =
    if (list.isEmpty) Future.successful(Seq.empty)
    else grunts.countByAuthor().map(counts => list.map(u => UserView.from(u, counts.getOrElse(u.id, 0), viewerId)))
}

object AccountService {
  val HandlePattern = "^[A-Za-z0-9_]{1,30}$".r
  val MinPasswordLength = 8

  /** Handles that would collide with the web app's own pages. */
  val ReservedHandles = Set("api", "assets", "team", "login", "signup", "logout", "settings", "favicon")

  def validate(fullName: String, handle: String, password: String): Option[String] =
    if (fullName.isEmpty) Some("Enter your name.")
    else if (fullName.length > 60) Some("That name is too long (60 characters at most).")
    else if (HandlePattern.findFirstIn(handle).isEmpty) Some("A handle is 1 to 30 letters, numbers or underscores.")
    else if (ReservedHandles.contains(handle.toLowerCase)) Some("That handle is taken.")
    else if (password.length < MinPasswordLength) Some(s"Use a password of at least $MinPasswordLength characters.")
    else None

  private def checkPassword(password: String, hash: String): Boolean =
    try BCrypt.checkpw(password, hash)
    catch { case _: IllegalArgumentException => false }
}
