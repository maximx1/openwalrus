package repositories.mongo

import javax.inject.{Inject, Singleton}

import com.mongodb.MongoWriteException
import com.mongodb.ErrorCategory.DUPLICATE_KEY
import models.{Grunt, NewGrunt, Reader}
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Accumulators.sum
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.Sorts.{ascending, descending}
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates.{addToSet, combine, inc, set, setOnInsert}
import repositories.GruntRepository

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/**
 * Grunts, replies and regrunts, all in "grunts". A reply has replyTo; a regrunt has
 * regruntOf. The grunt they belong to keeps a count of each, so lists needn't count them.
 */
@Singleton
class MongoGruntRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends GruntRepository {
  import MongoGruntRepository.*

  private val coll = mongo.grunts
  private val anonymousViews = mongo.anonymousViews

  def create(grunt: NewGrunt): Future[Grunt] = {
    val parent = grunt.replyTo.flatMap(Ids.parse)
    val doc = new Document()
      .append("_id", new ObjectId())
      .append("userId", new ObjectId(grunt.authorId))
      .append("message", grunt.message)
      .append("mentions", grunt.mentions.flatMap(Ids.parse).asJava)
      .append("timestamp", grunt.createdAt)
      .append("friendsOnly", grunt.friendsOnly)
    parent.foreach(doc.append("replyTo", _))
    for {
      _ <- coll.insertOne(doc).toFuture()
      _ <- parent.fold(Future.unit)(p => bump(p, "replyCount", 1))
    } yield fromDocument(doc)
  }

  def findById(id: String): Future[Option[Grunt]] =
    Ids.parse(id).fold(Future.successful(None))(oid => coll.find(equal("_id", oid)).projection(Fields).headOption().map(_.map(fromDocument)))

  def findByIds(ids: Seq[String], reader: Reader): Future[Seq[Grunt]] = {
    val oids = ids.flatMap(Ids.parse)
    if (oids.isEmpty) Future.successful(Seq.empty)
    else coll.find(and(in("_id", oids*), visibleTo(reader))).projection(Fields).toFuture().map(_.map(fromDocument))
  }

  def delete(id: String): Future[Unit] =
    Ids.parse(id).fold(Future.unit) { oid =>
      coll.find(equal("_id", oid)).projection(include("replyTo", "regruntOf")).headOption().flatMap {
        case None => Future.unit
        case Some(d) =>
          for {
            replies <- coll.find(equal("replyTo", oid)).projection(include("_id")).toFuture()
            _ <- anonymousViews.deleteMany(in("gruntId", (oid +: replies.map(_.getObjectId("_id")))*)).toFuture()
            _ <- coll.deleteMany(or(equal("_id", oid), equal("replyTo", oid), equal("regruntOf", oid))).toFuture()
            _ <- Option(d.getObjectId("replyTo")).fold(Future.unit)(bump(_, "replyCount", -1))
            _ <- Option(d.getObjectId("regruntOf")).fold(Future.unit)(bump(_, "regruntCount", -1))
          } yield ()
      }
    }

  def timeline(authorIds: Seq[String], mentioning: Option[String], before: Option[Long], limit: Int, reader: Reader): Future[Seq[Grunt]] = {
    val byAuthor = in("userId", authorIds.flatMap(Ids.parse)*)
    val mentioningId = mentioning.flatMap(Ids.parse)
    legacyMentions(mentioningId).flatMap { legacy =>
      val who = mentioningId.fold(byAuthor) { m =>
        or(Seq(byAuthor, equal("mentions", m)) ++ Option.when(legacy.nonEmpty)(in("_id", legacy*))*)
      }
      val filter = and(Seq(who, exists("replyTo", false), visibleTo(reader)) ++ before.map(lt("timestamp", _))*)
      coll.find(filter).projection(Fields).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
    }
  }

  def activeSince(since: Long, limit: Int, reader: Reader): Future[Seq[Grunt]] = {
    val noticed = or(exists("viewedBy.0"), gt("anonViews", 0), gt("replyCount", 0), gt("regruntCount", 0))
    coll.find(and(gt("timestamp", since), exists("replyTo", false), exists("regruntOf", false), noticed, visibleTo(reader)))
      .projection(Fields).sort(descending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
  }

  def replies(gruntId: String, limit: Int): Future[Seq[Grunt]] =
    Ids.parse(gruntId).fold(Future.successful(Seq.empty[Grunt])) { oid =>
      coll.find(equal("replyTo", oid)).projection(Fields).sort(ascending("timestamp")).limit(limit).toFuture().map(_.map(fromDocument))
    }

  // One regrunt per person and grunt: the upsert finds theirs or makes it, and a unique
  // index (MongoDatabaseProvider) settles two at once. The count moves only for a new one.
  def regrunt(userId: String, gruntId: String, at: Long): Future[Unit] =
    (Ids.parse(userId), Ids.parse(gruntId)) match {
      case (Some(u), Some(g)) =>
        val made = setOnInsert("timestamp", at)
        coll.updateOne(and(equal("userId", u), equal("regruntOf", g)), combine(made, setOnInsert("friendsOnly", false)), UpdateOptions().upsert(true))
          .toFuture()
          .flatMap(r => if (r.getUpsertedId != null) bump(g, "regruntCount", 1) else Future.unit)
          .recover { case e: MongoWriteException if e.getError.getCategory == DUPLICATE_KEY => () }
      case _ => Future.unit
    }

  def unregrunt(userId: String, gruntId: String): Future[Unit] =
    (Ids.parse(userId), Ids.parse(gruntId)) match {
      case (Some(u), Some(g)) =>
        coll.deleteOne(and(equal("userId", u), equal("regruntOf", g))).toFuture()
          .flatMap(r => if (r.getDeletedCount > 0) bump(g, "regruntCount", -1) else Future.unit)
      case _ => Future.unit
    }

  def regruntedBy(userId: String, gruntIds: Seq[String]): Future[Set[String]] = {
    val oids = gruntIds.flatMap(Ids.parse)
    Ids.parse(userId).filter(_ => oids.nonEmpty).fold(Future.successful(Set.empty[String])) { u =>
      coll.find(and(equal("userId", u), in("regruntOf", oids*))).projection(include("regruntOf")).toFuture()
        .map(_.map(_.getObjectId("regruntOf").toHexString).toSet)
    }
  }

  def setFriendsOnly(gruntId: String, friendsOnly: Boolean): Future[Unit] =
    Ids.parse(gruntId).fold(Future.unit) { oid =>
      if (!friendsOnly) coll.updateOne(equal("_id", oid), set("friendsOnly", false)).toFuture().map(_ => ())
      else
        for {
          _ <- coll.deleteMany(equal("regruntOf", oid)).toFuture()
          _ <- coll.updateOne(equal("_id", oid), combine(set("friendsOnly", true), set("regruntCount", 0))).toFuture()
        } yield ()
    }

  // Who saw it is kept as a set of ids on the grunt, so a second look by the same person
  // doesn't count again. For a small group's grunts the set stays small.
  def recordViews(ids: Seq[String], viewerId: String, reader: Reader): Future[Unit] =
    Ids.parse(viewerId).fold(Future.unit) { viewer =>
      val filter = and(in("_id", ids.flatMap(Ids.parse)*), notEqual("userId", viewer), visibleTo(reader))
      coll.updateMany(filter, addToSet("viewedBy", viewer)).toFuture().map(_ => ())
    }

  // Each anonymous view is a document whose id is a hash of the grunt and the visitor's id,
  // so inserting it again fails and nothing is counted twice; the grunt keeps the counts.
  def recordAnonymousViews(ids: Seq[String], visitor: String, site: Option[String], exceptAuthor: Option[String], at: Long): Future[Unit] = {
    val oids = ids.flatMap(Ids.parse)
    val notTheAuthor = exceptAuthor.flatMap(Ids.parse).map(notEqual("userId", _)).toSeq
    if (oids.isEmpty) Future.unit
    else
      coll.find(and((Seq(in("_id", oids*), exists("regruntOf", false), visibleTo(Reader.SignedOut)) ++ notTheAuthor)*))
        .projection(include("_id")).toFuture().flatMap { found =>
          Future.sequence(found.map(_.getObjectId("_id")).map { g =>
            val view = new Document("_id", viewKey(g, visitor)).append("gruntId", g).append("at", at)
            site.foreach(view.append("site", _))
            val counts = inc("anonViews", 1) +: site.map(_ => inc("embedViews", 1)).toSeq
            anonymousViews.insertOne(view).toFuture()
              .flatMap(_ => coll.updateOne(equal("_id", g), combine(counts*)).toFuture())
              .map(_ => ())
              .recover { case e: MongoWriteException if e.getError.getCategory == DUPLICATE_KEY => () }
          }).map(_ => ())
        }
  }

  def embedSites(gruntId: String, limit: Int): Future[Seq[(String, Int)]] =
    Ids.parse(gruntId).fold(Future.successful(Seq.empty[(String, Int)])) { oid =>
      anonymousViews.aggregate(Seq(
        Aggregates.filter(and(equal("gruntId", oid), exists("site"))),
        Aggregates.group("$site", sum("views", 1)),
        Aggregates.sort(descending("views")),
        Aggregates.limit(limit)
      )).toFuture().map(_.map(d => d.getString("_id") -> d.getInteger("views").intValue))
    }

  /** Grunts and replies; a regrunt isn't something its regrunter wrote. */
  def countByAuthor(): Future[Map[String, Int]] =
    coll.aggregate(Seq(Aggregates.filter(exists("regruntOf", false)), Aggregates.group("$userId", sum("count", 1)))).toFuture().map { docs =>
      docs.map(d => d.getObjectId("_id").toHexString -> d.getInteger("count").intValue).toMap
    }

  private def bump(id: ObjectId, field: String, by: Int): Future[Unit] =
    coll.updateOne(equal("_id", id), inc(field, by)).toFuture().map(_ => ())

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
}

object MongoGruntRepository {
  /** What a grunt is read with: everything but the list of who viewed it, which is counted instead. */
  val Fields: Document = new Document()
    .append("userId", 1)
    .append("message", 1)
    .append("mentions", 1)
    .append("timestamp", 1)
    .append("friendsOnly", 1)
    .append("replyTo", 1)
    .append("regruntOf", 1)
    .append("replyCount", 1)
    .append("regruntCount", 1)
    .append("views", new Document("$size", new Document("$ifNull", java.util.List.of[Object]("$viewedBy", java.util.List.of()))))
    .append("anonViews", 1)
    .append("embedViews", 1)

  /** Not the visitor's id itself, which stays in their browser. */
  def viewKey(gruntId: ObjectId, visitor: String): String =
    MessageDigest.getInstance("SHA-256").digest(s"${gruntId.toHexString}:$visitor".getBytes(UTF_8)).map("%02x".format(_)).mkString

  def fromDocument(d: Document): Grunt = Grunt(
    id = d.getObjectId("_id").toHexString,
    authorId = d.getObjectId("userId").toHexString,
    message = Option(d.getString("message")).getOrElse(""),
    mentions = Ids.list(d, "mentions"),
    createdAt = Ids.long(d, "timestamp"),
    friendsOnly = d.getBoolean("friendsOnly", false),
    signedInViews = Ids.long(d, "views").toInt,
    replyTo = Option(d.getObjectId("replyTo")).map(_.toHexString),
    regruntOf = Option(d.getObjectId("regruntOf")).map(_.toHexString),
    replyCount = math.max(0L, Ids.long(d, "replyCount")).toInt,
    regruntCount = math.max(0L, Ids.long(d, "regruntCount")).toInt,
    anonymousViews = Ids.long(d, "anonViews").toInt,
    embedViews = Ids.long(d, "embedViews").toInt
  )
}
