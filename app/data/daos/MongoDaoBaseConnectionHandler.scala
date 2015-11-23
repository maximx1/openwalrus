package data.daos

import com.mongodb.casbah.Imports._
import play.api.Application

/**
 * Base Mongo connection handler.
 * @author maximx1
 */
trait MongoDaoBaseConnectionHandler {
  val mongodbNameProp = "mongodb.default.name"
  
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
   * @param collectionName The name of the collection to open.
   * @return A Mongo Collection object.
   */
  protected def connect(collectionName: String)(implicit app: Application): MongoCollection = getCollection(getDB(MongoDaoBaseConnectionHandler.openClient), collectionName)
}

object MongoDaoBaseConnectionHandler {
  val mongodbURI = "mongodb.default.host"
  private var _client: Option[MongoClient] = None
  
  /**
   * Creates a singleton instance of of the connection pooler.
   * @param app the Application context from play.
   * @return The stored instance of the connection pooler.
   */
  def createInstance()(implicit app: Application) = _client.getOrElse {
    _client = Some(MongoClient(MongoClientURI(app.configuration.getString(mongodbURI).get)))
    _client.get
  }
  
  /**
   * Opens a client connection to configured db.
   * @return The Mongo client connection.
   */
  def openClient()(implicit app: Application) = createInstance
  
  /**
   * Closes the database connection.
   */
  def closeConnection = {
    _client = None
  }
}