package walrath.technology.openwalrus.daos

import play.api.Application
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User

/**
 * @author maximx1
 */
class UserDao()(implicit app: Application) extends MongoCRUDBase[User] {
  protected override val collName = "persons"
  
  def login(handle: String, password: String): Option[User] = {
    mongoColl.findOne(MongoDBObject("handle"->handle, "password"->password)) match {
    case Some(x) => Some(User.fromMongoObject(x))
    case None => None
  }}
}

