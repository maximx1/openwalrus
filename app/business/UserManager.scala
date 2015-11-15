package business

import java.io.File

import org.bson.types.ObjectId
import walrath.technology.openwalrus.model.tos.{UserTO, User, GruntTO}
import walrath.technology.openwalrus.daos.{FileDao, UserDao}
import play.api.Logger
import javax.inject.Inject
import com.google.inject.ImplementedBy
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

import walrath.technology.openwalrus.utils.ImageUtils

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
  def getUserProfile(handle: String): (Option[User], Option[List[GruntTO]], Map[String, UserTO])

  /**
   * Obtains all the users as a map using their key.
   * @param grunts The grunts to grab all users for.
   * @return Map of objectId to user.
   */
  def getGruntProfiles(grunts: List[GruntTO]): Map[String, UserTO]

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
   * @return true if the entry was successful, false otherwise.
   */
  def createUser(user: User, file: Option[(String, File)]): Boolean
}

/**
 * Implementation of business logic for user operations.
 * @author maximx1
 */
class UserManagerImpl @Inject() (userDao: UserDao, fileDao: FileDao) extends UserManager {
  val userNotFoundErrMsg = "User not found"
  val userAuthFailErrMsg = "User(%s) failed to authenticate"
  
  /**
   * Finds a user profile and gets the latest grunts.
   * @param handle The user's handle.
   * @return The User if exists and some of the latest grunts
   */
  def getUserProfile(handle: String): (Option[User], Option[List[GruntTO]], Map[String, UserTO]) = {
    
    userDao.findByHandle(handle) match {
      case Some(x) => {
        val grunts = GruntTO(x.id.get, x.handle, x.fullName, "8Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 8) :: 
                 GruntTO(x.id.get, x.handle, x.fullName, "4Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 4) ::
                 GruntTO(x.id.get, x.handle, x.fullName, "7Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 7) ::
               	 GruntTO(x.id.get, x.handle, x.fullName, "5Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 5) ::
                 GruntTO(x.id.get, x.handle, x.fullName, "3Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 3) ::
               	 GruntTO(x.id.get, x.handle, x.fullName, "My first grunt!", 0) ::
                 GruntTO(x.id.get, x.handle, x.fullName, "2Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 2) ::
               	 GruntTO(x.id.get, x.handle, x.fullName, "6Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 6) ::
                 GruntTO(x.id.get, x.handle, x.fullName, "1Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", 1) ::
                 Nil
        (Some(x), Some(grunts.sortBy(-_.timestamp)), getGruntProfiles(grunts))
      }
      case _ => (None, None, Map.empty)
    }
  }

  /**
   * Obtains all the users as a map using their key.
   * @param grunts The grunts to grab all users for.
   * @return Map of objectId to user.
   */
  def getGruntProfiles(grunts: List[GruntTO]): Map[String, UserTO] = {
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
   * @return true if the entry was successful, false otherwise.
   */
  def createUser(user: User, file: Option[(String, File)]): Boolean = {
    userDao ++ insertPhoto(file, true)
    .map{ case (imageRef, thumbRef) => {
      user.copy(profileImage=if(thumbRef.isEmpty) imageRef else thumbRef, images=imageRef.toList ++: thumbRef.toList)
    }}
    .getOrElse(user)
    .copy(password=BCrypt.hashpw(user.password, BCrypt.gensalt()), creationDate=CURRENT_TIMESTAMP)
    return true
  }

  /**
   * Inserts a photo and returns a reference to it and optionally a reference to it's thumb.
   * @param createThumb A flag to create a thumbnail or not.
   * @return A reference to the inserted image and optionally a reference to it's thumb.
   */
  def insertPhoto(file: Option[(String, File)], createThumb: Boolean): Option[(Option[ObjectId], Option[ObjectId])] = file.map(x => {
      val original = fileDao.store(x._2, x._1)
      val thumb = createThumb match {
        case true => fileDao.store(ImageUtils.imageToThumb(x._2).get, x._1)
        case true => None
      }
      (original, thumb)
    })

  
  def CURRENT_TIMESTAMP = (new Date()).getTime
}