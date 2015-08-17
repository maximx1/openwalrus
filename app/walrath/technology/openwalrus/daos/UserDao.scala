package walrath.technology.openwalrus.daos

import play.api.Application
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User

/**
 * @author maximx1
 */
class UserDao()(implicit app: Application) extends MongoDaoBase {
  private val collName = "persons"
  
  def mongoColl = connect(collName)
  
  def storeUser(user: User) = mongoColl.insert(user.toMongoDBObject)
  
  def findAll = mongoColl.find()
  
  def count = mongoColl.count()
}

