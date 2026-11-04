package repositories.mongo

import javax.inject.{Inject, Singleton}

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.bson.Document
import org.bson.types.Binary
import org.mongodb.scala.{Observable, ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.gridfs.GridFSBucket
import org.mongodb.scala.model.Filters.{and, equal, gte}
import org.mongodb.scala.model.Sorts.ascending
import repositories.{StoredVideo, VideoRepository}

import java.nio.ByteBuffer
import java.nio.file.{Files, Path}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Videos in the "owvideos" GridFS bucket; the id handed out is the file's. GridFS keeps a
 * file as numbered chunks of a fixed size, so reading from an offset (a browser seeking
 * through a video asks for a byte range) starts at the chunk holding it, not the beginning.
 */
@Singleton
class MongoVideoRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends VideoRepository {

  private val bucket = GridFSBucket(mongo.database, "owvideos")
  private val chunks = mongo.collection("owvideos.chunks")

  def store(file: Path, contentType: String, fileName: String): Future[String] = {
    val options = new GridFSUploadOptions().metadata(new Document("contentType", contentType))
    bucket.uploadFromObservable(fileName, Observable(Seq(ByteBuffer.wrap(Files.readAllBytes(file)))), options).head().map(_.toHexString)
  }

  def open(id: String): Future[Option[StoredVideo]] =
    Ids.parse(id).fold(Future.successful(None)) { fileId =>
      bucket.find(equal("_id", fileId)).headOption().map(_.map { file =>
        val chunkSize = file.getChunkSize.toLong
        val contentType = Option(file.getMetadata).flatMap(m => Option(m.getString("contentType"))).getOrElse("video/mp4")
        StoredVideo(file.getLength, contentType, offset => {
          val first = offset / chunkSize
          // A few chunks (255 KB each) fetched at a time, as the response is sent.
          val found = chunks.find(and(equal("files_id", fileId), gte("n", first))).sort(ascending("n")).batchSize(4)
          (first * chunkSize, Source.fromPublisher(found).map(d => ByteString(d.get("data", classOf[Binary]).getData)))
        })
      })
    }
}
