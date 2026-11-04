package repositories.mongo

import javax.inject.{Inject, Singleton}

import models.{NewUser, User}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Updates.*
import repositories.UserRepository

import java.util.regex.Pattern
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/**
 * Users in the "persons" collection. Field names match the 2015 documents; fields that
 * version wrote and nothing reads any more (email, phone, grunts, ...) are left alone.
 */
@Singleton
class MongoUserRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends UserRepository {
  import MongoUserRepository.*

  private val coll = mongo.users

  def create(user: NewUser): Future[User] = {
    val id = new ObjectId()
    val doc = new Document()
      .append("_id", id)
      .append("handle", user.handle)
      .append("fullName", user.fullName)
      .append("password", user.passwordHash)
      .append("creationDate", user.createdAt)
      .append("currentlyActivated", true)
      .append("profileImage", null)
      .append("bannerImage", null)
      .append("following", new java.util.ArrayList[ObjectId]())
      .append("followers", new java.util.ArrayList[ObjectId]())
    coll.insertOne(doc).toFuture().map(_ => fromDocument(doc))
  }

  def findById(id: String): Future[Option[User]] =
    Ids.parse(id).fold(Future.successful(None))(oid => coll.find(equal("_id", oid)).headOption().map(_.map(fromDocument)))

  def findByHandle(handle: String): Future[Option[User]] = findByHandles(Seq(handle)).map(_.headOption)

  def findByHandles(handles: Seq[String]): Future[Seq[User]] =
    if (handles.isEmpty) Future.successful(Seq.empty)
    else coll.find(in("handle", handles.map(handlePattern)*)).toFuture().map(_.map(fromDocument))

  def findByIds(ids: Seq[String]): Future[Seq[User]] = {
    val oids = ids.flatMap(Ids.parse)
    if (oids.isEmpty) Future.successful(Seq.empty)
    else coll.find(in("_id", oids*)).toFuture().map(_.map(fromDocument))
  }

  def all(): Future[Seq[User]] = coll.find().toFuture().map(_.map(fromDocument))

  def setProfileImage(userId: String, imageId: String): Future[Unit] = setField(userId, "profileImage", imageId)

  def setBannerImage(userId: String, imageId: String): Future[Unit] = setField(userId, "bannerImage", imageId)

  def setPasswordHash(userId: String, hash: String): Future[Unit] =
    Ids.parse(userId).fold(Future.unit)(u => coll.updateOne(equal("_id", u), set("password", hash)).toFuture().map(_ => ()))

  def follow(userId: String, targetId: String): Future[Unit] = (Ids.parse(userId), Ids.parse(targetId)) match {
    case (Some(u), Some(t)) =>
      for {
        _ <- coll.updateOne(equal("_id", u), addToSet("following", t)).toFuture()
        _ <- coll.updateOne(equal("_id", t), addToSet("followers", u)).toFuture()
      } yield ()
    case _ => Future.unit
  }

  def unfollow(userId: String, targetId: String): Future[Unit] = (Ids.parse(userId), Ids.parse(targetId)) match {
    case (Some(u), Some(t)) =>
      for {
        _ <- coll.updateOne(equal("_id", u), pull("following", t)).toFuture()
        _ <- coll.updateOne(equal("_id", t), pull("followers", u)).toFuture()
      } yield ()
    case _ => Future.unit
  }

  private def setField(userId: String, field: String, imageId: String): Future[Unit] =
    (Ids.parse(userId), Ids.parse(imageId)) match {
      case (Some(u), Some(i)) => coll.updateOne(equal("_id", u), set(field, i)).toFuture().map(_ => ())
      case _ => Future.unit
    }
}

object MongoUserRepository {
  /** Whole-handle, case-insensitive. (The 2015 version matched any handle containing it.) */
  private def handlePattern(handle: String): Pattern =
    Pattern.compile("^" + Pattern.quote(handle) + "$", Pattern.CASE_INSENSITIVE)

  def fromDocument(d: Document): User = User(
    id = d.getObjectId("_id").toHexString,
    handle = d.getString("handle"),
    fullName = Option(d.getString("fullName")).getOrElse(""),
    passwordHash = d.getString("password"),
    createdAt = Ids.long(d, "creationDate"),
    profileImage = Option(d.getObjectId("profileImage")).map(_.toHexString),
    bannerImage = Option(d.getObjectId("bannerImage")).map(_.toHexString),
    following = Ids.list(d, "following"),
    followers = Ids.list(d, "followers")
  )
}

/** Small helpers for moving between string ids and ObjectIds. */
private[mongo] object Ids {
  def parse(id: String): Option[ObjectId] = Option.when(ObjectId.isValid(id))(new ObjectId(id))

  def list(d: Document, key: String): Seq[String] =
    Option(d.getList(key, classOf[ObjectId])).map(_.asScala.toSeq.map(_.toHexString)).getOrElse(Seq.empty)

  def long(d: Document, key: String): Long = d.get(key) match {
    case n: java.lang.Number => n.longValue
    case _ => 0L
  }
}
