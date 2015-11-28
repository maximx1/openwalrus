package data.daos

import javax.inject.Inject

import com.google.inject.ImplementedBy
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.TypeImports
import models.User
import play.api.Application

/**
 * User Dao to handle db operations for Users.
 */
@ImplementedBy(classOf[UserMongoDao])
trait UserDao {
  /**
   * Finds a user document by the handle.
   * @param handle The user handle to key off.
   * @return The Optional User object found.
   */
  def findByHandle(handle: String): Option[User] = findByHandles(List(handle)).headOption

  /**
    * Finds a list of users by their handles.
    * @param handles The list of handles to look up.
    * @return The list of users found by those handles.
    */
  def findByHandles(handles: List[String]): List[User]
  
  /**
   * Finds a user by the user's Id.
   * @param id The Id.
   * @return user if found.
   */
  def findById(id: ObjectId): Option[User]
  
  /**
   * Finds a list of users by the user's Ids.
   * @param ids List of user ids.
   * @return List of all matching users.
   */
  def findByIds(ids: List[ObjectId]): List[User]
  
  /**
   * Add to database.
   * @param user The user to enter.
   * @return The new id entered.
   */
  def ++(user: User): Option[ObjectId]

  /**
    * Adds a grunt to a user's grunt lists.
    * @param userId The user Id to add the grunt to.
    * @param gruntId The gruntId to insert.
    * @return The result.
    */
  def addGrunt(userId: ObjectId, gruntId: ObjectId): Boolean = addGrunts(List(userId), gruntId)

  /**
    * Adds a grunt to all requested users' grunt lists.
    * @param userIds List of user ids to add this grunt to.
    * @param gruntId The gruntId to insert.
    * @return The result.
    */
  def addGrunts(userIds: List[ObjectId], gruntId: ObjectId): Boolean
  
  /**
   * Updates a user's profile image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  def updateProfileImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId]
  
  /**
   * Updates a user's banner image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  def updateBannerImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId]
  
  /**
   * Retrieves all users from the database.
   * return All users found.
   */
  def all(): List[User]
}

/**
 * Implementation for UserDao using Mongo.
 * @author maximx1
 */
class UserMongoDao @Inject() ()(implicit app: Application) extends MongoCRUDBase[User] with UserDao {
  protected override val collName = "persons"

  /**
    * Finds a list of users by their handles.
    * @param handles The list of handles to look up.
    * @return The list of users found by those handles.
    */
  override def findByHandles(handles: List[String]): List[User] =
    mongoColl.find(("handle" $in handles.map(x => ("(?i)" + x).r))).map(User.fromMongoObject(_)).toList

  /**
   * Finds a user by the user's Id.
   * @param id The message Id.
   * @return user if found.
   */
  override def findById(id: ObjectId): Option[User] =
    mongoColl.findOne(MongoDBObject("_id"->id)).map(User.fromMongoObject(_))

  /**
   * Finds a list of users by the user's Ids.
   * @param ids List of user ids.
   * @return List of all matching users.
   */
  override def findByIds(ids: List[ObjectId]): List[User] = {
    mongoColl.find(("_id" $in ids)).map(User.fromMongoObject(_)).toList
  }

  /**
   * Add to database.
   * @param user The user to enter.
   * @return The new id entered.
   */
  override def ++(user: User): Option[ObjectId] = {
    val id = Some(new ObjectId)
    create(user.copy(id=id))
    return id
  }

  /**
    * Adds a grunt to all requested users' grunt lists.
    * @param userIds List of user ids to add this grunt to.
    * @param gruntId The gruntId to insert.
    * @return The result.
    */
  override def addGrunts(userIds: List[ObjectId], gruntId: ObjectId): Boolean = {
    mongoColl.update(("_id" $in userIds), $push("grunts" -> gruntId), multi=true)
    true
  }
  
  /**
   * Updates a user's profile image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  override def updateProfileImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId] = {
    mongoColl.update(MongoDBObject("_id"->userId), $set("profileImage" -> imageRef))
    Some(imageRef)
  }
  
  /**
   * Updates a user's banner image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  override def updateBannerImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId] = {
    mongoColl.update(MongoDBObject("_id"->userId), $set("bannerImage" -> imageRef))
    Some(imageRef)
  }
  
  /**
   * Retrieves all users from the database.
   * return All users found.
   */
  override def all(): List[User] = User.fromMongoObjectList(this.findAll.toList)
}

