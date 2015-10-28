package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
 * User Model
 * @author maximx1
 */
case class User(
    id: Option[ObjectId],
    handle: String,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    creationDate: Long,
    currentlyActivated: Boolean
  ) extends BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  override def toMongoDBObject = MongoDBObject(
    "handle"->handle,
    "email"->email,
    "password"->password,
    "firstName"->firstName,
    "lastName"->lastName,
    "creationDate"->creationDate,
    "currentlyActivated"->currentlyActivated
  )
}

/**
 * Helper object for User Model.
 * @author maximx1
 */
object User extends BaseModel[User] {
  /**
   * Row mapper to map the mongodb object to the case class.
   * @param mongoObject The DBObject to deserialize.
   * @return The deserialized object.
   */
  override def fromMongoObject(mongoObject: DBObject): User = User(
    Some(mongoObject.as[ObjectId]("_id")),
    mongoObject.as[String]("handle"),
    mongoObject.as[String]("email"),
    mongoObject.as[String]("password"),
    mongoObject.as[String]("firstName"),
    mongoObject.as[String]("lastName"),
    mongoObject.as[Long]("creationDate"),
    mongoObject.as[Boolean]("currentlyActivated")
  )
}