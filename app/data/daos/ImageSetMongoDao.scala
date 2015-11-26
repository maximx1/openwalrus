package data.daos

import com.google.inject.ImplementedBy
import javax.inject.Inject
import models.ImageSet
import com.mongodb.casbah.Imports._
import play.api.Application

/**
 * Dao for containing different image sizes.
 */
@ImplementedBy(classOf[ImageSetMongoDao])
trait ImageSetDao {
  /**
   * Add to database.
   * @param imageSet The imageSet to enter.
   * @return The new id entered.
   */
  def ++(imageSet: ImageSet): Option[ObjectId]
  
  /**
   * Finds a ImageSet by the ImageSet's Id.
   * @param id The Id.
   * @return imageSet if found.
   */
  def findById(id: ObjectId): Option[ImageSet]
  
  /**
   * Finds a ImageSets by the ImageSets' Id.
   * @param ids The Ids.
   * @return list of found imageSets.
   */
  def findByIds(ids: ObjectId): List[ImageSet]
}

class ImageSetMongoDao @Inject() ()(implicit app: Application) extends MongoCRUDBase[ImageSet] with ImageSetDao {
  protected override val collName = "imageset"
  
  /**
   * Add to database.
   * @param imageSet The imageSet to enter.
   * @return The new id entered.
   */
  override def ++(imageSet: ImageSet): Option[ObjectId] = {
    val id = Some(new ObjectId)
    create(imageSet.copy(id=id))
    return id
  }
  
  /**
   * Finds a ImageSet by the ImageSet's Id.
   * @param id The Id.
   * @return imageSet if found.
   */
  def findById(id: ObjectId): Option[ImageSet] = mongoColl.findOne(MongoDBObject("_id"->id)).map(ImageSet.fromMongoObject(_))
  
  /**
   * Finds a ImageSets by the ImageSets' Id.
   * @param ids The Ids.
   * @return list of found imageSets.
   */
  def findByIds(ids: ObjectId): List[ImageSet] = mongoColl.find(("_id" $in ids)).map(ImageSet.fromMongoObject(_)).toList
}