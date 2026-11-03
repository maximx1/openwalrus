package repositories.mongo

import javax.inject.{Inject, Singleton}

import com.mongodb.ConnectionString
import org.bson.Document
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase, ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.Indexes
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logging}
import repositories.DatabaseHealth

import scala.concurrent.{ExecutionContext, Future}

/**
 * The one MongoDB client for the app, built from `mongodb.uri` (the MONGO_URI environment
 * variable). The database is the one named in the URI's path, or "openwalrus".
 */
@Singleton
class MongoDatabaseProvider @Inject() (config: Configuration, lifecycle: ApplicationLifecycle)(using ec: ExecutionContext)
    extends DatabaseHealth with Logging {

  private val uri = config.get[String]("mongodb.uri")
  private val client = MongoClient(uri)
  val database: MongoDatabase = client.getDatabase(Option(ConnectionString(uri).getDatabase).getOrElse("openwalrus"))

  def collection(name: String): MongoCollection[Document] = database.getCollection[Document](name)

  // Collection names are the ones the 2015 version used, so an old database still reads.
  val users: MongoCollection[Document] = collection("persons")
  val grunts: MongoCollection[Document] = collection("grunts")
  val imageSets: MongoCollection[Document] = collection("imageset")

  // Idempotent, and Mongo builds them in the background. A failure is logged rather than
  // stopping the app, so it still starts while the database is briefly unreachable.
  Future.sequence(Seq(
    grunts.createIndex(Indexes.descending("userId", "timestamp")).toFuture(),
    grunts.createIndex(Indexes.ascending("mentions")).toFuture(),
    grunts.createIndex(Indexes.descending("timestamp")).toFuture()
  )).failed.foreach(e => logger.warn(s"Could not create indexes: ${e.getMessage}"))

  lifecycle.addStopHook(() => Future.successful(client.close()))

  def ping(): Future[Unit] = database.runCommand(Document("ping", 1)).toFuture().map(_ => ())
}
