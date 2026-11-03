package repositories.mongo

import javax.inject.{Inject, Singleton}

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import models.StoredImage
import org.bson.Document
import org.bson.types.ObjectId
import org.mongodb.scala.{Observable, ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.gridfs.GridFSBucket
import org.mongodb.scala.model.Filters.equal
import repositories.ImageRepository

import java.nio.ByteBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * Image bytes in the "owimages" GridFS bucket, paired up (original and thumbnail) by a
 * document in "imageset". The id handed out is the imageset's.
 */
@Singleton
class MongoImageRepository @Inject() (mongo: MongoDatabaseProvider)(using ec: ExecutionContext) extends ImageRepository {

  private val bucket = GridFSBucket(mongo.database, "owimages")
  private val sets = mongo.imageSets

  def store(original: StoredImage, thumb: StoredImage, fileName: String): Future[String] =
    for {
      originalId <- upload(original, fileName)
      thumbId <- upload(thumb, "thumb_" + fileName)
      id = new ObjectId()
      _ <- sets.insertOne(new Document("_id", id).append("original", originalId).append("thumb", thumbId)).toFuture()
    } yield id.toHexString

  def find(id: String, thumb: Boolean): Future[Option[StoredImage]] =
    Ids.parse(id).fold(Future.successful(None)) { oid =>
      sets.find(equal("_id", oid)).headOption().flatMap {
        case None => Future.successful(None)
        case Some(set) =>
          Option(set.getObjectId(if (thumb) "thumb" else "original")).orElse(Option(set.getObjectId("original"))) match {
            case None => Future.successful(None)
            case Some(fileId) => download(fileId)
          }
      }
    }

  private def upload(image: StoredImage, fileName: String): Future[ObjectId] = {
    val options = new GridFSUploadOptions().metadata(new Document("contentType", image.contentType))
    bucket.uploadFromObservable(fileName, Observable(Seq(ByteBuffer.wrap(image.bytes))), options).head()
  }

  private def download(fileId: ObjectId): Future[Option[StoredImage]] =
    bucket.find(equal("_id", fileId)).headOption().flatMap {
      case None => Future.successful(None)
      case Some(file) =>
        // The 2015 version stored the type as a top-level field, which this driver no longer
        // reads, so fall back to the file name for those.
        val contentType = Option(file.getMetadata).flatMap(m => Option(m.getString("contentType")))
          .getOrElse(services.ImageService.typeForName(file.getFilename))
        bucket.downloadToObservable(fileId).toFuture().map { chunks =>
          val out = new java.io.ByteArrayOutputStream()
          chunks.foreach { b =>
            val arr = new Array[Byte](b.remaining)
            b.get(arr)
            out.write(arr)
          }
          Some(StoredImage(out.toByteArray, contentType))
        }
    }
}
