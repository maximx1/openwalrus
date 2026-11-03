package repositories.mongo

import javax.inject.{Inject, Singleton}

import models.{Grunt, NewGrunt}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Accumulators.sum
import org.mongodb.scala.model.Aggregates.group
import org.mongodb.scala.model.Filters.*
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

  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int): Future[Seq[Grunt]] = {
    val byAuthor = in("userId", authorIds.flatMap(Ids.parse)*)
    val who = mentioning.flatMap(Ids.parse).fold(byAuthor)(m => or(byAuthor, equal("mentions", m)))
    val filter = before.fold(who)(b => and(who, lt("timestamp", b)))
    coll.find(filter).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
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
