package models

import com.mongodb.casbah.Imports._
import core.utils.TypeUtils.nullToNone

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
    verified: Boolean,
    profileImage: Option[ObjectId],
    bannerImage: Option[ObjectId],
    images: List[ObjectId],
    grunts: List[ObjectId],
    following: List[ObjectId],
    followers: List[ObjectId]
  ) extends BaseTOModel {
  /**
   * Mapper to serialize object to MongoDBObject
   * @return The serialized object.
   */
  override def toMongoDBObject = MongoDBObject(
    "_id"->id.getOrElse(new ObjectId),
    "handle"->handle,
    "email"->email,
    "phone"->phone,
    "password"->password,
    "fullName"->fullName,
    "creationDate"->creationDate,
    "currentlyActivated"->currentlyActivated,
    "verified"->verified,
    "profileImage"->profileImage,
    "bannerImage"->bannerImage,
    "images"->images,
    "grunts"->grunts,
    "following"->following,
    "followers"->followers
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
    mongoObject.as[Boolean]("verified"),
    nullToNone(Some(mongoObject.as[ObjectId]("profileImage"))),
    nullToNone(Some(mongoObject.as[ObjectId]("bannerImage"))),
    mongoObject.as[List[ObjectId]]("images"),
    mongoObject.as[List[ObjectId]]("grunts"),
    mongoObject.as[List[ObjectId]]("following"),
    mongoObject.as[List[ObjectId]]("followers")
  )
}