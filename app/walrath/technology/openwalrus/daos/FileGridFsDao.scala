package walrath.technology.openwalrus.daos

import java.io.File
import com.mongodb.casbah.gridfs.Imports._
import com.mongodb.casbah.Imports._
import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Application
import java.io.InputStream

import walrath.technology.openwalrus.utils.ImageUtils

case class ImageData(inputStream: InputStream, filename: Option[String], contentType: Option[String])

/**
 * File Storage for images and possibly video in the future.
 */
@ImplementedBy(classOf[FileGridFsDao])
trait FileDao {
  /**
   * Stores a file into database.
   * @param file The file to insert.
   * @return The new Id
   */
  def store(file: File, fileName: String): Option[ObjectId]

  /**
   * Stores a file into database.
   * @param file The file to insert.
   * @return The new Id
   */
  def store(file: Array[Byte], fileName: String): Option[ObjectId]

  /**
   * Attempts to retrieve a file from the database.
   * @param id The file id to retrieve.
   */
  def retrieve(id: ObjectId): Option[ImageData]
}

class FileGridFsDao @Inject() ()(implicit app: Application) extends GridFsBaseConnectionHandler with FileDao {
  val gridBucket: GridFS = connectGridFs("owimages")
  
  /**
   * Stores a file into database.
   * @param file The file to insert.
   * @return The new Id
   */
  def store(file: File, fileName: String): Option[ObjectId] = gridBucket(file){ f=>
    f.filename = file.getName
    f.contentType = ImageUtils.determineWebType(file.getName()).getOrElse("image/png")
  } match {
    case Some(x: ObjectId) => Some(x)
    case _ => None
  }

  /**
   * Stores a file into database.
   * @param file The file to insert.
   * @return The new Id
   */
  def store(file: Array[Byte], fileName: String): Option[ObjectId] = gridBucket(file){ f=>
    f.filename = fileName
    f.contentType = ImageUtils.determineWebType(fileName).getOrElse("image/png")
  } match {
    case Some(x: ObjectId) => Some(x)
    case _ => None
  }
  
  /**
   * Attempts to retrieve a file from the database.
   * @param id The file id to retrieve.
   * @return The stream of the file.
   */
  def retrieve(id: ObjectId): Option[ImageData] = gridBucket.findOne(id).map(x => ImageData(x.inputStream, x.filename, x.contentType))
}