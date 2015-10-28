package walrath.technology.openwalrus.daos

import com.mongodb.casbah.Imports._
import play.api.Application

/**
 * Base Mongo connection handler.
 * @author maximx1
 */
trait MongoDaoBaseConnectionHandler {
  val mongodbURI = "mongodb.default.host"
  val mongodbNameProp = "mongodb.default.name"
  
  /**
   * Opens a client connection to configured db.
   * @return The Mongo client connection.
   */
  protected def openClient()(implicit app: Application) = MongoClient(MongoClientURI(app.configuration.getString(mongodbURI).get))
  
  /**
   * Gets the Mongo DB object.
   * @param client The Mongo client connection.
   * @return The Mongo DB objects.
   */
  protected def getDB(client: MongoClient)(implicit app: Application): MongoDB = client(app.configuration.getString(mongodbNameProp).get)
  
  /**
   * Gets a Mongo Collection object.
   * @param db Mongo db object to connect to.
   * @param collectionName The name of the collection to open.
   * @return A Mongo Collection object.
   */
  protected def getCollection(db: MongoDB, collectionName: String)(implicit app: Application): MongoCollection = db(collectionName)
  
  /**
   * Simple wrapper to connect to configured db and open the collection for the name passed in.
   * @collectionName The name of the collection to open.
   * @return A Mongo Collection object.
   */
  protected def connect(collectionName: String)(implicit app: Application): MongoCollection = getCollection(getDB(openClient), collectionName)
}