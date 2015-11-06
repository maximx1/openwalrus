package walrath.technology.openwalrus.daos

import java.io.File
import com.mongodb.casbah.gridfs.Imports._
import com.mongodb.casbah.Imports._
import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Application
import java.io.InputStream

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
  def store(file: File): Option[ObjectId]
  
  /**
   * Attempts to retrieve a file from the database.
   * @param retrieve The file id to retrieve.
   */
  def retrieve(objectId: String): Option[InputStream]
}

class FileGridFsDao @Inject() ()(implicit app: Application) extends GridFsBaseConnectionHandler with FileDao {
  val gridBucket: GridFS = connectGridFs("owimages")
  
  /**
   * Stores a file into database.
   * @param file The file to insert.
   * @return The new Id
   */
  def store(file: File): Option[ObjectId] = gridBucket(file){f=>} match {
      case Some(x: ObjectId) => Some(x)
      case _ => None
    }
  
  /**
   * Attempts to retrieve a file from the database.
   * @param retrieve The file id to retrieve.
   */
  def retrieve(id: String): Option[InputStream] = gridBucket.findOne(new ObjectId(id)).map(_.inputStream)
}