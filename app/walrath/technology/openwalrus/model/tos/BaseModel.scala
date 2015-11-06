package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
 * Base trait for the helper Object to the Model.
 * @author maximx1
 */
trait BaseModel[T <: BaseTOModel] {
  /**
   * Row mapper to map the mongodb object to the case class.
   * @param mongoObject The DBObject to deserialize.
   * @return The deserialized object.
   */
  def fromMongoObject(mongoObject: DBObject): T
  
  /**
   * List row mapper to map all rows.
   * @param mongoObjects List of DBObjects to be deserialized in batch.
   * @return The list of deserialized objects.
   */
  def fromMongoObjectList(mongoObjects: List[DBObject]): List[T] = mongoObjects.map(fromMongoObject(_))
}

/**
 * The Base Model to extend the case class based models from.
 */
trait BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  def toMongoDBObject: MongoDBObject
}