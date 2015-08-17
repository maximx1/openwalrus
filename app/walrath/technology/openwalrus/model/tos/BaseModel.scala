package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
 * @author maximx1
 */
trait BaseModel[T] {
  /**
   * Row mapper to map the mongodb object to the case class.
   */
  def fromMongoObject(mongoObject: DBObject): T
  
  /**
   * List row mapper to map all rows.
   */
  def fromMongoObjectList(mongoObjects: List[DBObject]): List[T] = mongoObjects.map(fromMongoObject(_))
}