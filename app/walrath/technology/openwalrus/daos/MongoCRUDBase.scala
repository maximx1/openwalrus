package walrath.technology.openwalrus.daos

import play.api.Application
import walrath.technology.openwalrus.model.tos.BaseTOModel
import com.mongodb.casbah.Imports._

/**
 * A base Mongo operations class with some primitive CRUD functionality.
 * @author maximx1
 */
abstract class MongoCRUDBase[T <: BaseTOModel]()(implicit app: Application) extends MongoDaoBaseConnectionHandler {
  
  /**
   * The backing object for the main collection for the Dao.
   */
  protected var _mongoColl: Option[MongoCollection] = None
  
  /**
   * The name of the main collection for the Dao.
   */
  protected def collName: String
  
  /**
   * The main collection for the Dao.
   * @return The main collection.
   */
  protected def mongoColl: MongoCollection = _mongoColl match {
    case Some(x) => x
    case None => { val x = connect(collName); _mongoColl = Some(x); x }
  }
  
  /**
   * Creates a new document in the database based on the serialized input.
   * @param t The input object matching the instance's Dao type.
   * @return The write result.
   */
  def create(t: T) = mongoColl.insert(t.toMongoDBObject)
  
  /**
   * Select * operation to grab all the documents for the collection.
   * @return All the documents for the collection from the db.
   */
  def findAll = mongoColl.find()
  
  /**
   * Gets the count of documents stored in the collection.
   * @return The count of documents.
   */
  def count = mongoColl.count()
}