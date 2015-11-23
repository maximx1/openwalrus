package data.daos

import com.mongodb.casbah.gridfs.Imports._
import play.api.Application

/**
 * Base Gridfs connection handler.
 * @author maximx1
 */
trait GridFsBaseConnectionHandler extends MongoDaoBaseConnectionHandler {
  
  /**
   * Simple wrapper to connect to configured db and open the gridfs bucket for the name passed in.
   * @param bucket The name of the bucket to open.
   * @return A Mongo Collection object.
   */
  protected def connectGridFs(bucket: String)(implicit app: Application): GridFS = GridFS(getDB(MongoDaoBaseConnectionHandler.openClient), bucket)
}