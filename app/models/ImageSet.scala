package models

import com.mongodb.casbah.Imports._
import core.utils.TypeUtils.nullToNone

/**
 * Image container for mongo.
 */
case class ImageSet(
  id: Option[ObjectId],
  original: ObjectId,
  thumb: Option[ObjectId]
) extends BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  override def toMongoDBObject = MongoDBObject(
    "_id"->id.getOrElse(new ObjectId),
    "original"->original,
    "thumb"->thumb
  )
}

object ImageSet extends BaseModel[ImageSet] {
  def fromMongoObject(mongoObject: DBObject): ImageSet = ImageSet(
    nullToNone(Some(mongoObject.as[ObjectId]("_id"))),
    mongoObject.as[ObjectId]("original"),
    nullToNone(Some(mongoObject.as[ObjectId]("thumb")))
  )
}