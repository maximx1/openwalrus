package business

import java.io.File
import data.daos.{GruntDao, UserDao, FileDao}
import org.bson.types.ObjectId
import models.{Grunt, UserTO, User, GruntTO}
import play.api.Logger
import javax.inject.Inject
import com.google.inject.ImplementedBy
import org.mindrot.jbcrypt.BCrypt
import core.utils.DateUtils.CURRENT_TIMESTAMP
import core.utils.ImageUtils
import data.daos.UserMongoDao

/**
 * Business logic for User operations.
 */
@ImplementedBy(classOf[UserManagerImpl])
trait UserManager {
  /**
   * Finds a user profile and gets the latest grunts.
   * @param handle The user's handle.
   * @return The User if exists and some of the latest grunts
   */
  def getUserProfile(handle: String): (Option[UserTO], List[GruntTO], Map[String, UserTO])

  /**
   * Gets all of the users' profile data.
   * @return All userTOs.
   */
  def getUserProfiles: List[UserTO]
  
  /**
   * Obtains all the users as a map using their key.
   * @param grunts The grunts to grab all users for.
   * @return Map of objectId to user.
   */
  def getGruntProfiles(grunts: List[Grunt]): Map[String, UserTO]

  /**
   * Attempts the login of a user
   * @param handle The username to try
   * @param password The password to try
   * @return The User if matched.
   */
  def login(handle: String, password: String): Option[User]
  
  /**
   * Checks to see if the User with passed in handle exists.
   * @param handle The username to try
   * @return true if the handle exists, false if it doesn't.
   */
  def checkIfHandleInUse(handle: String): Boolean
  
  /**
   * Creates a new user.
   * @param user The new user to insert.
   * @param file The file information. Should be filename and file
   * @return The objectId if the entry was successful
   */
  def createUser(user: User): Option[ObjectId]

  /**
   * Inserts a photo and returns a reference to it and optionally a reference to it's thumb.
   * @param file The file and filename to insert.
   * @param createThumb A flag to create a thumbnail or not.
   * @return A reference to the inserted image and optionally a reference to it's thumb.
   */
  def insertPhoto(file: Option[(String, File)], createThumb: Boolean): Option[(Option[ObjectId], Option[ObjectId])]
  
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
   * Updates the relationship with the person signed in and whether or not they are following them.
   * If the user is following it unfollows, if user isn't following it follows.
   * @param userId The user's id.
   * @param followingId The user's id to follow/unfollow
   * @return true if following, false if not following.
   */
  def updateFollowing(userId: ObjectId, followingId: ObjectId): Boolean
  
  /**
   * Gets all of the users that are following the user.
   * @param handle The user handle to look up followers for.
   * @return The found followers.
   */
  def getFollowers(handle: String): List[UserTO]
  
  /**
   * Gets all of the users that the user is following.
   * @param handle The user handle to look up following for.
   * @return The found following.
   */
  def getFollowing(handle: String): List[UserTO]
}

/**
 * Implementation of business logic for user operations.
 * @author maximx1
 */
class UserManagerImpl @Inject() (userDao: UserDao, fileDao: FileDao, gruntDao: GruntDao) extends UserManager {
  val userNotFoundErrMsg = "User not found"
  val userAuthFailErrMsg = "User(%s) failed to authenticate"
  
  /**
   * Finds a user profile and gets the latest grunts.
   * @param handle The user's handle.
   * @return The User if exists and some of the latest grunts
   */
  def getUserProfile(handle: String): (Option[UserTO], List[GruntTO], Map[String, UserTO]) = {
    userDao.findByHandle(handle) match {
      case Some(u) => {
        val grunts: List[Grunt] = gruntDao.findByIds(u.grunts)
        val gruntProfiles = getGruntProfiles(grunts)
        val userTO = UserTO.fromUser(u, gruntDao.countByUser(u.id.get))
        (Some(userTO), grunts.map(g => GruntTO.fromGrunt(g, gruntProfiles(g.userId.toString))).sortBy(-_.timestamp), gruntProfiles)
      }
      case _ => (None, List.empty, Map.empty)
    }
  }

  /**
   * Obtains all the users as a map using their key.
   * @param grunts The grunts to grab all users for.
   * @return Map of objectId to user.
   */
  def getGruntProfiles(grunts: List[Grunt]): Map[String, UserTO] = {
    userDao.findByIds(grunts.map(_.userId)).map(x => x.id.get.toString -> UserTO.fromUser(x)).toMap
  }
  
  /**
   * Attempts the login of a user
   * @param handle The username to try
   * @param password The password to try
   * @return The User if matched.
   */
  def login(handle: String, password: String): Option[User] = {
    userDao.findByHandle(handle) match {
      case Some(x) => if(BCrypt.checkpw(password, x.password)) Some(x.copy(password="")) else { Logger.info(userAuthFailErrMsg format handle);None }
      case None => { Logger.info(userNotFoundErrMsg);None }
    }
  }
  
  /**
   * Checks to see if the User with passed in handle exists.
   * @param handle The username to try
   * @return true if the handle exists, false if it doesn't.
   */
  def checkIfHandleInUse(handle: String): Boolean = !userDao.findByHandle(handle).isEmpty
  
  /**
   * Creates a new user.
   * @param user The new user to insert.
   * @param file The file information. Should be filename and file
   * @return The objectId if the entry was successful
   */
  def createUser(user: User): Option[ObjectId] = {
    val newId = userDao ++ user.copy(password=BCrypt.hashpw(user.password, BCrypt.gensalt()), creationDate=CURRENT_TIMESTAMP)
    return newId
  }

  /**
   * Inserts a photo and returns a reference to it and optionally a reference to it's thumb.
   * @param file The file and filename to insert.
   * @param createThumb A flag to create a thumbnail or not.
   * @return A reference to the inserted image and optionally a reference to it's thumb.
   */
  def insertPhoto(file: Option[(String, File)], createThumb: Boolean): Option[(Option[ObjectId], Option[ObjectId])] = file.map(x => {
      val original = fileDao.store(x._2, x._1)
      val thumb = createThumb match {
        case true => fileDao.store(ImageUtils.imageToThumb(x._2).get, x._1)
        case false => None
      }
      (original, thumb)
    })
    
  /**
   * Updates a user's profile image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  def updateProfileImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId] = userDao.updateProfileImage(userId, imageRef)
  
  /**
   * Updates a user's banner image.
   * @param userId The user's id.
   * @param imageRef The new imageSet Id.
   * @return The imageSet id.
   */
  def updateBannerImage(userId: ObjectId, imageRef: ObjectId): Option[ObjectId] = userDao.updateBannerImage(userId, imageRef)
  
  /**
   * Gets all of the users' profile data.
   * @return All userTOs.
   */
  def getUserProfiles: List[UserTO] = userDao.all.map(x => UserTO.fromUser(x, gruntDao.countByUser(x.id.get)))
  
  /**
   * Updates the relationship with the person signed in and whether or not they are following them.
   * If the user is following it unfollows, if user isn't following it follows.
   * @param userId The user's id.
   * @param followingId The user's id to follow/unfollow
   * @return true if following, false if not following.
   */
  def updateFollowing(userId: ObjectId, followingId: ObjectId): Boolean = {
	  val acting: Option[User] = userDao.findById(userId)
    val actee: Option[User] = userDao.findById(followingId)
    println(acting)
    acting.map { actingUser =>
      actee match { 
        case Some(acteeUser) if(actingUser.following.contains(followingId)) => {
          userDao.removeFollowing(userId, followingId)
          userDao.removeFollower(followingId, userId)
          false
        }
        case Some(acteeUser) if(!actingUser.following.contains(followingId)) => {
          userDao.addFollowing(userId, followingId)
          userDao.addFollower(followingId, userId)
          true
        }
        case _ => false
      }
    }.getOrElse(false)
  }
  
  /**
   * Gets all of the users that are following the user.
   * @param handle The user handle to look up followers for.
   * @return The found followers.
   */
  def getFollowers(handle: String): List[UserTO] = {
    userDao.findByHandle(handle).map { user => 
      userDao.findByIds(user.followers).map(x => UserTO.fromUser(x, gruntDao.countByUser(x.id.get)))
    }.getOrElse(List.empty)
  }
  
  /**
   * Gets all of the users that the user is following.
   * @param handle The user handle to look up following for.
   * @return The found following.
   */
  def getFollowing(handle: String): List[UserTO] = {
    userDao.findByHandle(handle).map { user => 
      userDao.findByIds(user.following).map(x => UserTO.fromUser(x, gruntDao.countByUser(x.id.get)))
    }.getOrElse(List.empty)
  }
}