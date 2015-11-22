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

/**
  * Helper object to convert Grunts
  */
object GruntTO{
  /**
    * Converts a grunt to a gruntTO using the user data.
    * @param g The grunt.
    * @param u The user.
    * @return The converted GruntTO.
    */
  def fromGrunt(g: Grunt, u: UserTO) = GruntTO(g.userId, u.handle, u.fullName, g.message, g.timestamp)
}