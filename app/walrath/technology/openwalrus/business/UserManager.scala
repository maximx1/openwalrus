package walrath.technology.openwalrus.business

import walrath.technology.openwalrus.model.tos.User
import walrath.technology.openwalrus.daos.UserDao
import walrath.technology.openwalrus.daos.UserMongoDao
import play.api.{Application, Logger}
import javax.inject.Inject
import com.google.inject.ImplementedBy
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

/**
 * Business logic for User operations.
 */
@ImplementedBy(classOf[UserManagerImpl])
trait UserManager {
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
   * @return true if the entry was successful, false otherwise.
   */
  def createUser(user: User): Boolean
}

/**
 * Implementation of business logic for user operations.
 * @author maximx1
 */
class UserManagerImpl @Inject() (userDao: UserDao) extends UserManager {
  val userNotFoundErrMsg = "User not found"
  val userAuthFailErrMsg = "User(%s) failed to authenticate"
  
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
   * @return true if the entry was successful, false otherwise.
   */
  def createUser(user: User): Boolean = {
    val newId = userDao ++ user.copy(password=BCrypt.hashpw(user.password, BCrypt.gensalt()), creationDate=CURRENT_TIMESTAMP)
    return true
  }
  
  def CURRENT_TIMESTAMP = (new Date()).getTime
}