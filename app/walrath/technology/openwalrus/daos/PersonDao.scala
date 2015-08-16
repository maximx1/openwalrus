package walrath.technology.openwalrus.daos

import play.api.Application
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.Person

/**
 * @author maximx1
 */
class PersonDao()(implicit app: Application) extends MongoDaoBase {
  private val collName = "persons"
  
  def mongoColl = connect(collName)
  
  def storeUser(person: Person) = mongoColl.insert(person.toMongoDBObject)
  
  def findAll = mongoColl.find()
  
  def count = mongoColl.count()
}

