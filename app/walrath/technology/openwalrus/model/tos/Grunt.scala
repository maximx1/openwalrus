package walrath.technology.openwalrus.model.tos

import com.mongodb.casbah.Imports._

/**
 * Message container.
 */
case class Grunt(userId: ObjectId, handle: String, fullName: String, message: String, timestamp: String)