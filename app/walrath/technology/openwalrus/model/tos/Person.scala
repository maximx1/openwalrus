package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
 * @author maximx1
 */
case class Person(id: Option[ObjectId], firstName: String, lastName: String) {
  def toMongoDBObject = MongoDBObject("firstName"->firstName, "lastName"->lastName)
}

/**
 * @author maximx1
 */
object Person {
  def fromMongoObject(mongoObject: DBObject): Person = {
    Person(Some(mongoObject.as[ObjectId]("_id")), mongoObject.as[String]("firstName"), mongoObject.as[String]("lastName"))
  }
  def fromMongoObjectList(mongoObjects: List[DBObject]): List[Person] = mongoObjects.map(fromMongoObject(_))
}