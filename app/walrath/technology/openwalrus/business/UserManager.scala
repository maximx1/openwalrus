package walrath.technology.openwalrus.business

import walrath.technology.openwalrus.model.tos.User
import walrath.technology.openwalrus.daos.UserDao
import walrath.technology.openwalrus.daos.UserMongoDao
import play.api.{Application, Logger}
import javax.inject.Inject

/**
 * Business side of the User calls.
 * @author maximx1
 */
class UserManager @Inject() (userDao: UserDao) {
  val userNotFoundErrMsg = "User not found"
  val userAuthFailErrMsg = "User(%s) failed to authenticate"
  
  /**
   * Attempts the login of a user
   */
  def login(handle: String, password: String): Option[User] = {
    userDao.findByHandle(handle) match {
      case Some(x) => if(x.password == password) Some(x.copy(password="")) else { Logger.info(userAuthFailErrMsg format handle);None }
      case None => { Logger.info(userNotFoundErrMsg);None }
    }
  }
}