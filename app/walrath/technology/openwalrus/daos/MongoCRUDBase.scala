package walrath.technology.openwalrus.daos

import play.api.Application
import walrath.technology.openwalrus.model.tos.BaseTOModel
import com.mongodb.casbah.Imports._

/**
 * @author maximx1
 */
abstract class MongoCRUDBase[T <: BaseTOModel]()(implicit app: Application) extends MongoDaoBase {
  protected var _mongoColl: Option[MongoCollection] = None
  protected def collName: String
  protected def mongoColl: MongoCollection = _mongoColl match {
    case Some(x) => x
    case None => { val x = connect(collName); _mongoColl = Some(x); x }
  }
  def create(t: T) = mongoColl.insert(t.toMongoDBObject)
  def findAll = mongoColl.find()
  def count = mongoColl.count()
}