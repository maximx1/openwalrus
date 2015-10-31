package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.utils.TypeUtils.nullToNone

/**
 * User Model
 * @author maximx1
 */
case class User(
    id: Option[ObjectId],
    handle: String,
    email: Option[String],
    phone: Option[String],
    password: String,
    fullName: String,
    creationDate: Long,
    currentlyActivated: Boolean,
    verified: Boolean
  ) extends BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  override def toMongoDBObject = MongoDBObject(
    "handle"->handle,
    "email"->email,
    "phone"->phone,
    "password"->password,
    "fullName"->fullName,
    "creationDate"->creationDate,
    "currentlyActivated"->currentlyActivated,
    "verified"->verified
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
    nullToNone(Some(mongoObject.as[String]("email"))),
    nullToNone(Some(mongoObject.as[String]("phone"))),
    mongoObject.as[String]("password"),
    mongoObject.as[String]("fullName"),
    mongoObject.as[Long]("creationDate"),
    mongoObject.as[Boolean]("currentlyActivated"),
    mongoObject.as[Boolean]("verified")
  )
}