package repositories

import models.*
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import java.nio.file.Path
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
  def setPasswordHash(userId: String, hash: String): Future[Unit]
  /** Records both sides: userId follows targetId, and targetId is followed by userId. */
  def follow(userId: String, targetId: String): Future[Unit]
  def unfollow(userId: String, targetId: String): Future[Unit]
}

trait GruntRepository {
  def create(grunt: NewGrunt): Future[Grunt]
  def findById(id: String): Future[Option[Grunt]]
  /** Any order; leaves out the ones the reader may not see. */
  def findByIds(ids: Seq[String], reader: Reader): Future[Seq[Grunt]]
  /** Deletes the grunt, and with it its replies and regrunts. */
  def delete(id: String): Future[Unit]
  /**
   * Newest first, older than `before`: grunts and regrunts by any of the authors, and
   * grunts mentioning the user. No replies. Like every list here, it leaves out
   * friends-only grunts the reader may not see.
   */
  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int, reader: Reader): Future[Seq[Grunt]]
  /** Newest first: grunts (not replies) posted after `since` that someone has seen, answered or regrunted. */
  def activeSince(since: Long, limit: Int, reader: Reader): Future[Seq[Grunt]]
  /** Oldest first. */
  def replies(gruntId: String, limit: Int): Future[Seq[Grunt]]
  /** Once per person; regrunting again changes nothing. */
  def regrunt(userId: String, gruntId: String, at: Long): Future[Unit]
  def unregrunt(userId: String, gruntId: String): Future[Unit]
  /** Which of the grunts the user has regrunted. */
  def regruntedBy(userId: String, gruntIds: Seq[String]): Future[Set[String]]
  /** Making a grunt friends-only also undoes its regrunts, which would show it to others. */
  def setFriendsOnly(gruntId: String, friendsOnly: Boolean): Future[Unit]
  /** Counts a view of each grunt by the viewer: once per person, and never by the author. */
  def recordViews(ids: Seq[String], viewerId: String, reader: Reader): Future[Unit]
  /**
   * Counts a view of each grunt by someone not signed in, known only by an id their browser
   * made up: once per id and grunt. Only grunts anyone may see, and none by `exceptAuthor`.
   * Through an embed, `site` is where it's embedded ("" when that isn't known).
   */
  def recordAnonymousViews(ids: Seq[String], visitor: String, site: Option[String], exceptAuthor: Option[String], at: Long): Future[Unit]
  /** The sites a grunt is embedded on, with the views from each, most first. */
  def embedSites(gruntId: String, limit: Int): Future[Seq[(String, Int)]]
  /** Number of grunts each user has written. Users with none are left out. */
  def countByAuthor(): Future[Map[String, Int]]
}

trait SessionRepository {
  def create(session: NewSession): Future[SessionRecord]
  def findByTokenHash(tokenHash: String): Future[Option[SessionRecord]]
  /** Notes the session as used now, and pushes back when it ends for lack of use. */
  def touch(id: String, at: Long, expiresAt: Long): Future[Unit]
  def forUser(userId: String): Future[Seq[SessionRecord]]
  /** Ends one of the user's sessions. False if they have no session with that id. */
  def delete(userId: String, id: String): Future[Boolean]
  /** Ends all the user's sessions but `except`. */
  def deleteAll(userId: String, except: Option[String]): Future[Unit]
}

trait ImageRepository {
  /** Stores an image and its thumbnail together, and returns the id that finds either. */
  def store(original: StoredImage, thumb: StoredImage, fileName: String): Future[String]
  def find(id: String, thumb: Boolean): Future[Option[StoredImage]]
  /** The original's size and type, without reading it. */
  def describe(id: String): Future[Option[MediaFile]]
}

/** How big a stored picture or video is, and what kind of file. */
case class MediaFile(length: Long, contentType: String)

/**
 * A stored video, read as a stream so a large one never sits in memory whole. readFrom
 * starts at or before the byte offset asked for and says where it actually starts.
 */
case class StoredVideo(length: Long, contentType: String, readFrom: Long => (Long, Source[ByteString, ?]))

trait VideoRepository {
  /** Stores the file as it is and returns the id that finds it. */
  def store(file: Path, contentType: String, fileName: String): Future[String]
  def open(id: String): Future[Option[StoredVideo]]
}

/** Lets the health check ask whether the database is answering. */
trait DatabaseHealth {
  def ping(): Future[Unit]
}
