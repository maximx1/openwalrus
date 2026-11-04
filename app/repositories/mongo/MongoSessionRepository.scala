package repositories.mongo

import javax.inject.{Inject, Singleton}

import models.{NewSession, SessionRecord}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Updates.{combine, set}
import repositories.SessionRepository

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

/** Sessions in the "sessions" collection. expiresAt is a Date so a TTL index can clear them out. */
@Singleton
class MongoSessionRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends SessionRepository {
  import MongoSessionRepository.*

  private val coll = mongo.sessions

  def create(s: NewSession): Future[SessionRecord] = {
    val doc = new Document()
      .append("_id", new ObjectId())
      .append("tokenHash", s.tokenHash)
      .append("userId", new ObjectId(s.userId))
      .append("createdAt", s.createdAt)
      .append("lastSeenAt", s.createdAt)
      .append("expiresAt", new Date(s.expiresAt))
      .append("userAgent", s.userAgent)
      .append("address", s.address)
    coll.insertOne(doc).toFuture().map(_ => fromDocument(doc))
  }

  def findByTokenHash(tokenHash: String): Future[Option[SessionRecord]] =
    coll.find(equal("tokenHash", tokenHash)).headOption().map(_.map(fromDocument))

  def touch(id: String, at: Long, expiresAt: Long): Future[Unit] =
    Ids.parse(id).fold(Future.unit) { oid =>
      coll.updateOne(equal("_id", oid), combine(set("lastSeenAt", at), set("expiresAt", new Date(expiresAt)))).toFuture().map(_ => ())
    }

  def forUser(userId: String): Future[Seq[SessionRecord]] =
    Ids.parse(userId).fold(Future.successful(Seq.empty[SessionRecord]))(u => coll.find(equal("userId", u)).toFuture().map(_.map(fromDocument)))

  def delete(userId: String, id: String): Future[Boolean] =
    (Ids.parse(userId), Ids.parse(id)) match {
      case (Some(u), Some(s)) => coll.deleteOne(and(equal("_id", s), equal("userId", u))).toFuture().map(_.getDeletedCount > 0)
      case _ => Future.successful(false)
    }

  def deleteAll(userId: String, except: Option[String]): Future[Unit] =
    Ids.parse(userId).fold(Future.unit) { u =>
      val keep = except.flatMap(Ids.parse).map(notEqual("_id", _)).toSeq
      coll.deleteMany(and(equal("userId", u) +: keep*)).toFuture().map(_ => ())
    }
}

object MongoSessionRepository {
  def fromDocument(d: Document): SessionRecord = SessionRecord(
    id = d.getObjectId("_id").toHexString,
    userId = d.getObjectId("userId").toHexString,
    createdAt = Ids.long(d, "createdAt"),
    lastSeenAt = Ids.long(d, "lastSeenAt"),
    expiresAt = Option(d.getDate("expiresAt")).map(_.getTime).getOrElse(0L),
    userAgent = Option(d.getString("userAgent")).getOrElse(""),
    address = Option(d.getString("address")).getOrElse("")
  )
}
