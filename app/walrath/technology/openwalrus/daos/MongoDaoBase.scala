package walrath.technology.openwalrus.daos

import com.mongodb.casbah.Imports._
import play.api.Application

/**
 * @author maximx1
 */
trait MongoDaoBase {
  //val
  val mongodbURI = "mongodb.default.host"
  val mongodbNameProp = "mongodb.default.name"
  protected def openClient()(implicit app: Application) = MongoClient(MongoClientURI(app.configuration.getString(mongodbURI).get))
  protected def getDB(client: MongoClient)(implicit app: Application): MongoDB = client(app.configuration.getString(mongodbNameProp).get)
  protected def getCollection(db: MongoDB, collectionName: String)(implicit app: Application): MongoCollection = db(collectionName)
  protected def connect(collectionName: String)(implicit app: Application): MongoCollection = getCollection(getDB(openClient), collectionName)
}