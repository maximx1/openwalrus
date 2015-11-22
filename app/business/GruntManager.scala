package business

import javax.inject.Inject
import com.google.inject.ImplementedBy
import core.utils.MessageUtils
import data.daos.{UserDao, GruntDao}
import core.utils.DateUtils.CURRENT_TIMESTAMP
import models.Grunt
import org.bson.types.ObjectId
import models.GruntTO
import models.UserTO

/**
  * Business logic for Grunt operations.
  */
@ImplementedBy(classOf[GruntManagerImpl])
trait GruntManager {
  /**
    * Inserts a new post and attaches it to a user.
    * @param newGrunt The new grunt to insert.
    * @return The new object if inserted correctly.
    */
  def insertNewGrunt(newGrunt: Grunt): Option[ObjectId]
  
  /**
   * Gets a grunt by it's id along with the user data.
   * @param id The grunt id to look up.
   * @return The combined gruntTO and the userTO if found.
   */
  def getGruntById(id: ObjectId): Option[(GruntTO, UserTO)]
}

class GruntManagerImpl @Inject() (gruntDao: GruntDao, userDao: UserDao) extends GruntManager {

  /**
    * Inserts a new post and attaches it to a user.
    * @param newGrunt The new grunt to insert.
    * @return The new object if inserted correctly.
    */
  def insertNewGrunt(newGrunt: Grunt): Option[ObjectId] = {
    (gruntDao ++ newGrunt.copy(
        timestamp = CURRENT_TIMESTAMP,
        message = MessageUtils.htmlToTextWithNewLines(newGrunt.message))).map { id: ObjectId =>
      val users = userDao.findByHandles(MessageUtils.pullAtUsers(newGrunt.message))
      userDao.addGrunts(users.map(_.id.get) :+ newGrunt.userId, id)
      id
    }
  }
  
  /**
   * Gets a grunt by it's id along with the user data.
   * @param id The grunt id to look up.
   * @return The combined gruntTO and the userTO if found.
   */
  def getGruntById(id: ObjectId): Option[(GruntTO, UserTO)] = {
    gruntDao.findById(id).map{ g => 
        userDao.findById(g.userId).map{u =>
          val uTO = UserTO.fromUser(u)
          (GruntTO.fromGrunt(g, uTO), uTO)
        }
    }.getOrElse(None)
  }
}