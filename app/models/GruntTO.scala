package models

import org.bson.types.ObjectId
import io.github.gitbucket.markedj.Marked
import models.json.Jsonify

/**
 * Simple transfer object to pass back and forth with all information.
 */
case class GruntTO(
    userId: ObjectId,
    handle: String,
    fullName: String,
    message: String,
    timestamp: Long
)  extends Jsonify

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
  def fromGrunt(g: Grunt, u: UserTO) = GruntTO(g.userId, u.handle, u.fullName, Marked.marked(g.message), g.timestamp)
}