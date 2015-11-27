package business

import com.google.inject.ImplementedBy
import java.io.File
import data.daos.FileGridFsDao
import javax.inject.Inject
import org.bson.types.ObjectId
import core.utils.ImageUtils
import data.daos.ImageSetDao
import models.ImageSet
import data.daos.ImageData

/**
 * Basic File operation manager.
 */
@ImplementedBy(classOf[FileManagerImpl])
trait FileManager {
  /**
   * Stores an image into the db.
   * @param file The file.
   * @param fileName The name of the file on the client's side.
   * @return The id of the new file.
   */
  def storeImage(file: File, fileName: String): Option[ObjectId]
  
  /**
   * Looks up an image
   * @param key The objectId to lookup.
   * @param isThumb boolean indicating if a thumbnail version is needed.
   * @return The ImageData from the database.
   */
  def lookUpImage(key: ObjectId, isThumb: Boolean): Option[ImageData]
}

class FileManagerImpl @Inject() (fileGridFsDao: FileGridFsDao, imageSetDao: ImageSetDao) extends FileManager {
  
  /**
   * Stores an image into the db.
   * @param file The file.
   * @param fileName The name of the file on the client's side.
   * @return The id of the new ImageSet.
   */
  def storeImage(file: File, fileName: String): Option[ObjectId] = {
    ImageUtils.determineWebType(fileName).map { webType =>
      val thumb= ImageUtils.imageToThumb(file).get
      val originalId = fileGridFsDao.store(file, fileName)
      val thumbId = fileGridFsDao.store(thumb, "thumb_" + fileName)
      imageSetDao ++ ImageSet(None, originalId.get, thumbId)
    }.flatten
  }
  
  /**
   * Looks up an image
   * @param key The objectId to lookup.
   * @param isThumb boolean indicating if a thumbnail version is needed.
   * @return The ImageData from the database.
   */
  def lookUpImage(key: ObjectId, isThumb: Boolean): Option[ImageData] = {
    imageSetDao.findById(key).map { imageSet =>
      isThumb match {
        case true => fileGridFsDao.retrieve(imageSet.thumb.get)
        case false => fileGridFsDao.retrieve(imageSet.original)
      }
    }.flatten
  }
}