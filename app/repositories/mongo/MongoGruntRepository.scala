package repositories.mongo

import javax.inject.{Inject, Singleton}

import models.{Grunt, NewGrunt, Reader}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Accumulators.sum
import org.mongodb.scala.model.Aggregates.group
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.Sorts.descending
import org.mongodb.scala.model.Updates.addToSet
import repositories.GruntRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@Singleton
class MongoGruntRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends GruntRepository {
  import MongoGruntRepository.*

  private val coll = mongo.grunts

  def create(grunt: NewGrunt): Future[Grunt] = {
    val doc = new Document()
      .append("_id", new ObjectId())
      .append("userId", new ObjectId(grunt.authorId))
      .append("message", grunt.message)
      .append("mentions", grunt.mentions.flatMap(Ids.parse).asJava)
      .append("timestamp", grunt.createdAt)
      .append("friendsOnly", grunt.friendsOnly)
    coll.insertOne(doc).toFuture().map(_ => fromDocument(doc))
  }

  def findById(id: String): Future[Option[Grunt]] =
    Ids.parse(id).fold(Future.successful(None))(oid => coll.find(equal("_id", oid)).projection(Fields).headOption().map(_.map(fromDocument)))

  def delete(id: String): Future[Unit] =
    Ids.parse(id).fold(Future.unit)(oid => coll.deleteOne(equal("_id", oid)).toFuture().map(_ => ()))

  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int, reader: Reader): Future[Seq[Grunt]] = {
    val byAuthor = in("userId", authorIds.flatMap(Ids.parse)*)
    val mentioningId = mentioning.flatMap(Ids.parse)
    legacyMentions(mentioningId).flatMap { legacy =>
      val who = mentioningId.fold(byAuthor) { m =>
        or(Seq(byAuthor, equal("mentions", m)) ++ Option.when(legacy.nonEmpty)(in("_id", legacy*))*)
      }
      val filter = and(Seq(who, visibleTo(reader)) ++ before.map(lt("timestamp", _))*)
      coll.find(filter).projection(Fields).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
    }
  }

  def viewedSince(since: Long, limit: Int, reader: Reader): Future[Seq[Grunt]] =
    coll.find(and(gt("timestamp", since), exists("viewedBy.0"), visibleTo(reader)))
      .projection(Fields).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))

  // Who saw it is kept as a set of ids on the grunt, so a second look by the same person
  // doesn't count again. For a small group's grunts the set stays small.
  def recordViews(ids: Seq[String], viewerId: String, reader: Reader): Future[Unit] =
    Ids.parse(viewerId).fold(Future.unit) { viewer =>
      val filter = and(in("_id", ids.flatMap(Ids.parse)*), notEqual("userId", viewer), visibleTo(reader))
      coll.updateMany(filter, addToSet("viewedBy", viewer)).toFuture().map(_ => ())
    }

  /** Everyone's ordinary grunts, and friends-only ones from the authors the reader is a friend of. */
  private def visibleTo(reader: Reader) =
    or(notEqual("friendsOnly", true), in("userId", reader.friendOf.flatMap(Ids.parse)*))

  /**
   * The 2015 version didn't mark grunts with whom they mention. It added each grunt's id to
   * the mentioned user's own "grunts" list instead, so read that too. Empty for new users.
   */
  private def legacyMentions(userId: Option[ObjectId]): Future[Seq[ObjectId]] = userId match {
    case None => Future.successful(Seq.empty)
    case Some(id) =>
      mongo.users.find(equal("_id", id)).projection(include("grunts")).headOption().map {
        case Some(d) => Option(d.getList("grunts", classOf[ObjectId])).map(_.asScala.toSeq).getOrElse(Seq.empty)
        case None => Seq.empty
      }
  }

  def countByAuthor(): Future[Map[String, Int]] =
    coll.aggregate(Seq(group("$userId", sum("count", 1)))).toFuture().map { docs =>
      docs.map(d => d.getObjectId("_id").toHexString -> d.getInteger("count").intValue).toMap
    }
}

object MongoGruntRepository {
  /** What a grunt is read with: everything but the list of who viewed it, which is counted instead. */
  val Fields: Document = new Document()
    .append("userId", 1)
    .append("message", 1)
    .append("mentions", 1)
    .append("timestamp", 1)
    .append("friendsOnly", 1)
    .append("views", new Document("$size", new Document("$ifNull", java.util.List.of[Object]("$viewedBy", java.util.List.of()))))

  def fromDocument(d: Document): Grunt = Grunt(
    id = d.getObjectId("_id").toHexString,
    authorId = d.getObjectId("userId").toHexString,
    message = Option(d.getString("message")).getOrElse(""),
    mentions = Ids.list(d, "mentions"),
    createdAt = Ids.long(d, "timestamp"),
    friendsOnly = d.getBoolean("friendsOnly", false),
    views = Ids.long(d, "views").toInt
  )
}
