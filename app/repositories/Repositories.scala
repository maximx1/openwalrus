package repositories

import models.*
import scala.concurrent.Future

/**
 * Data access. The rest of the app only sees these traits; the MongoDB versions live in
 * repositories.mongo and are bound in Module. Another database means another set of
 * implementations and a change to that binding, nothing else.
 */
trait UserRepository {
  def create(user: NewUser): Future[User]
  def findById(id: String): Future[Option[User]]
  /** Case-insensitive, exact match. */
  def findByHandle(handle: String): Future[Option[User]]
  def findByHandles(handles: Seq[String]): Future[Seq[User]]
  def findByIds(ids: Seq[String]): Future[Seq[User]]
  def all(): Future[Seq[User]]
  def setProfileImage(userId: String, imageId: String): Future[Unit]
  def setBannerImage(userId: String, imageId: String): Future[Unit]
  /** Records both sides: userId follows targetId, and targetId is followed by userId. */
  def follow(userId: String, targetId: String): Future[Unit]
  def unfollow(userId: String, targetId: String): Future[Unit]
}

trait GruntRepository {
  def create(grunt: NewGrunt): Future[Grunt]
  def findById(id: String): Future[Option[Grunt]]
  /** Newest first: grunts by any of the authors or mentioning the user, older than `before`. */
  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int): Future[Seq[Grunt]]
  /** Number of grunts each user has written. Users with none are left out. */
  def countByAuthor(): Future[Map[String, Int]]
}

trait ImageRepository {
  /** Stores an image and its thumbnail together, and returns the id that finds either. */
  def store(original: StoredImage, thumb: StoredImage, fileName: String): Future[String]
  def find(id: String, thumb: Boolean): Future[Option[StoredImage]]
}

/** Lets the health check ask whether the database is answering. */
trait DatabaseHealth {
  def ping(): Future[Unit]
}
