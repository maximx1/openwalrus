package services

import javax.inject.{Inject, Singleton}

import models.{NewUser, User, UserView}
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
            users.create(NewUser(handle, fullName, PasswordHasher.hash(form.password), System.currentTimeMillis())).map(Right(_))
        }
    }
  }

  /** The user, if the password is theirs. An old-style hash is replaced on the way. */
  def signIn(handle: String, password: String): Future[Option[User]] =
    users.findByHandle(handle.trim.stripPrefix("@")).flatMap {
      case None =>
        PasswordHasher.wasteTime(password)
        Future.successful(None)
      case Some(user) if PasswordHasher.verify(password, user.passwordHash) =>
        val upgrade =
          if (PasswordHasher.needsRehash(user.passwordHash)) users.setPasswordHash(user.id, PasswordHasher.hash(password))
          else Future.unit
        upgrade.map(_ => Some(user))
      case Some(_) => Future.successful(None)
    }

  /** Left is a message for the user. Ending their other sessions is up to the caller. */
  def changePassword(user: User, current: String, next: String): Future[Either[String, Unit]] =
    if (!PasswordHasher.verify(current, user.passwordHash)) Future.successful(Left(WrongCurrentPassword))
    else if (next == current) Future.successful(Left("That's the password you have now."))
    else passwordProblem(next, user.handle) match {
      case Some(problem) => Future.successful(Left(problem))
      case None => users.setPasswordHash(user.id, PasswordHasher.hash(next)).map(Right(_))
    }

  /** Why a handle can't be had (badly formed, or someone has it), or None if it's free. */
  def handleProblem(raw: String): Future[Option[String]] = {
    val handle = raw.trim.stripPrefix("@")
    handleShapeProblem(handle) match {
      case Some(problem) => Future.successful(Some(problem))
      case None => users.findByHandle(handle).map(_.map(_ => "That handle is taken."))
    }
  }

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
  val MinPasswordLength = 10
  val MaxPasswordLength = 200
  val WrongCurrentPassword = "Your current password isn't right."

  /** Handles that would collide with the web app's own pages. */
  val ReservedHandles = Set("api", "assets", "team", "hot", "login", "signup", "logout", "settings", "favicon")

  /** Long ones that would pass the length rule; the short favourites already fail it. */
  private val CommonPasswords = Set(
    "1234567890", "0987654321", "12345678910", "123456789a", "a123456789", "1234567890a", "abcd123456",
    "qwertyuiop", "qwertyuiop1", "qwerty1234", "qwerty12345", "1q2w3e4r5t", "1qaz2wsx3edc", "zaq12wsxcde",
    "asdfghjkl1", "abcdefghij", "password12", "password123", "password1234", "password!1", "passw0rd123",
    "p@ssw0rd123", "p@ssword123", "iloveyou12", "iloveyou123", "letmein123", "welcome123", "welcome1234",
    "trustno1234", "sunshine123", "princess123", "football123", "baseball123", "superman123", "batman1234",
    "michael123", "monkey12345", "dragon12345", "master12345", "changeme123", "administrator", "openwalrus",
    "openwalrus1", "walrus1234"
  )
  def validate(fullName: String, handle: String, password: String): Option[String] =
    if (fullName.isEmpty) Some("Enter your name.")
    else if (fullName.length > 60) Some("That name is too long (60 characters at most).")
    else handleShapeProblem(handle).orElse(passwordProblem(password, handle))

  def passwordProblem(password: String, handle: String): Option[String] = {
    val lower = password.toLowerCase
    if (password.length < MinPasswordLength) Some(s"Use a password of at least $MinPasswordLength characters.")
    else if (password.length > MaxPasswordLength) Some(s"That password is too long ($MaxPasswordLength characters at most).")
    else if (handle.length >= 3 && lower.contains(handle.toLowerCase)) Some("Your password can't have your handle in it.")
    else if (password.distinct.length < 4 || CommonPasswords.contains(lower)) Some("That password is too easy to guess.")
    else None
  }

  def handleShapeProblem(handle: String): Option[String] =
    if (HandlePattern.findFirstIn(handle).isEmpty) Some("A handle is 1 to 30 letters, numbers or underscores.")
    else if (ReservedHandles.contains(handle.toLowerCase)) Some("That handle is taken.")
    else None
}
