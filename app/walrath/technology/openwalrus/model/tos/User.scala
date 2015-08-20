package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
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
 * @author maximx1
 */
object User extends BaseModel[User] {
  /**
   * Row mapper to map the mongodb object to the case class.
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