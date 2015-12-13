package data.daos

import com.google.inject.ImplementedBy
import com.mongodb.casbah.Imports._
import javax.inject.Inject
import models.Grunt
import play.api.Application

@ImplementedBy(classOf[GruntMongoDao])
trait GruntDao {
  
  /**
   * Add to database.
   * @param grunt The grunt to enter.
   * @return The new id entered.
   */
  def ++(grunt: Grunt): Option[ObjectId]
  
  /**
   * Finds a grunt by the user's Id.
   * @param userId The id of the user.
   * @return a list of grunts for the user.
   */
  def findByUserId(userId: ObjectId): List[Grunt]
  
  /**
   * Finds a grunt by the grunt's Id.
   * @param id The message Id.
   * @return Grunt if found.
   */
  def findById(id: ObjectId): Option[Grunt]
  
  /**
   * Finds a list of grunts by the grunt's Ids.
   * @param ids List of grunt ids.
   * @return List of all matching Grunts.
   */
  def findByIds(ids: List[ObjectId]): List[Grunt]
  
  /**
   * Gets a count of all the grunts originated by a specific user.
   * @param userId The user's id.
   * @return The count.
   */
  def countByUser(userId: ObjectId): Int
}

/**
 * Mongo Impl of Grunt Dao for operations retrieving storing grunts.
 */
class GruntMongoDao @Inject() ()(implicit app: Application) extends MongoCRUDBase[Grunt] with GruntDao {
  protected override val collName = "grunts"
  
  /**
   * Finds a grunt by the user's Id.
   * @param userId The id of the user.
   * @return a list of grunts for the user.
   */
  override def findByUserId(userId: ObjectId): List[Grunt] = 
    mongoColl.find(MongoDBObject("userId"->userId)).map(Grunt.fromMongoObject(_)).toList
    
    
  /**
   * Finds a grunt by the grunt's Id.
   * @param id The message Id.
   * @return Grunt if found.
   */
  override def findById(id: ObjectId): Option[Grunt] = 
    mongoColl.findOne(MongoDBObject("_id"->id)).map(Grunt.fromMongoObject(_))
  
  /**
   * Finds a list of grunts by the grunt's Ids.
   * @param ids List of grunt ids.
   * @return List of all matching Grunts.
   */
  override def findByIds(ids: List[ObjectId]): List[Grunt] = { 
    mongoColl.find(("_id" $in ids)).map(Grunt.fromMongoObject(_)).toList
  }
  
  /**
   * Add to database.
   * @param grunt The grunt to enter.
   * @return The new id entered.
   */
  override def ++(grunt: Grunt): Option[ObjectId] = {
    val id = Some(new ObjectId)
    create(grunt.copy(id=id))
    return id
  }
  
  /**
   * Gets a count of all the grunts originated by a specific user.
   * @param userId The user's id.
   * @return The count
   */
  def countByUser(userId: ObjectId): Int = mongoColl.find(MongoDBObject("userId"->userId)).count()
}