package walrath.technology.openwalrus.daos

import play.api.Application
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User
import com.google.inject.ImplementedBy
import javax.inject.Inject

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
  def findByHandle(handle: String): Option[User]
}

/**
 * Implementation for UserDao using Mongo.
 * @author maximx1
 */
class UserMongoDao @Inject() ()(implicit app: Application) extends MongoCRUDBase[User] with UserDao {
  protected override val collName = "persons"
  
  /**
   * Finds a user document by the handle.
   * @param handle The user handle to key off.
   * @return The Optional User object found.
   */
  override def findByHandle(handle: String): Option[User] = {
    mongoColl.findOne(MongoDBObject("handle"->handle)) match {
      case Some(x) => Some(User.fromMongoObject(x))
      case None => None
  }}
}

