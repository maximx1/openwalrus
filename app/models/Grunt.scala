package models

import com.mongodb.casbah.Imports._
import core.utils.TypeUtils.nullToNone

/**
 * Message container.
 */
case class Grunt(
    id: Option[ObjectId],
    userId: ObjectId,
    originalMessage: Option[ObjectId],
    regrunts: List[ObjectId],
    favorites: List[ObjectId],
    message: String,
    timestamp: Long
)  extends BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  override def toMongoDBObject = MongoDBObject(
    "_id"->id.getOrElse(new ObjectId),
    "userId"->userId,
    "originalMessage"->originalMessage,
    "regrunts"->regrunts,
    "favorites"->favorites,
    "message"->message,
    "timestamp"->timestamp
  )
}

/**
 * Helper object for User Model.
 * @author maximx1
 */
object Grunt extends BaseModel[Grunt] {
  /**
   * Row mapper to map the mongodb object to the case class.
   * @param mongoObject The DBObject to deserialize.
   * @return The deserialized object.
   */
  override def fromMongoObject(mongoObject: DBObject): Grunt = Grunt(
    Some(mongoObject.as[ObjectId]("_id")),
    mongoObject.as[ObjectId]("userId"),
    nullToNone(Some(mongoObject.as[ObjectId]("originalMessage"))),
    mongoObject.as[List[ObjectId]]("regrunts"),
    mongoObject.as[List[ObjectId]]("favorites"),
    mongoObject.as[String]("message"),
    mongoObject.as[Long]("timestamp")
  )
}