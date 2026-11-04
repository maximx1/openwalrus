package repositories.mongo

import javax.inject.{Inject, Singleton}

import models.{Grunt, NewGrunt}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Accumulators.sum
import org.mongodb.scala.model.Aggregates.group
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.Sorts.descending
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
    coll.insertOne(doc).toFuture().map(_ => fromDocument(doc))
  }

  def findById(id: String): Future[Option[Grunt]] =
    Ids.parse(id).fold(Future.successful(None))(oid => coll.find(equal("_id", oid)).headOption().map(_.map(fromDocument)))

  def delete(id: String): Future[Unit] =
    Ids.parse(id).fold(Future.unit)(oid => coll.deleteOne(equal("_id", oid)).toFuture().map(_ => ()))

  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int): Future[Seq[Grunt]] = {
    val byAuthor = in("userId", authorIds.flatMap(Ids.parse)*)
    val mentioningId = mentioning.flatMap(Ids.parse)
    legacyMentions(mentioningId).flatMap { legacy =>
      val who = mentioningId.fold(byAuthor) { m =>
        or(Seq(byAuthor, equal("mentions", m)) ++ Option.when(legacy.nonEmpty)(in("_id", legacy*))*)
      }
      val filter = before.fold(who)(b => and(who, lt("timestamp", b)))
      coll.find(filter).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
    }
  }

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
  def fromDocument(d: Document): Grunt = Grunt(
    id = d.getObjectId("_id").toHexString,
    authorId = d.getObjectId("userId").toHexString,
    message = Option(d.getString("message")).getOrElse(""),
    mentions = Ids.list(d, "mentions"),
    createdAt = Ids.long(d, "timestamp")
  )
}
