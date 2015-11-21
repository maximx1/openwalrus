package models

import com.mongodb.casbah.Imports._

/**
 * Simple transfer object to pass back and forth with all information.
 */
case class GruntTO(
    userId: ObjectId,
    handle: String,
    fullName: String,
    message: String,
    timestamp: Long
)
