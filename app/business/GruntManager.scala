package business

import javax.inject.Inject
import com.google.inject.ImplementedBy
import core.utils.MessageUtils
import data.daos.{UserDao, GruntDao}
import core.utils.DateUtils.CURRENT_TIMESTAMP
import models.Grunt
import org.bson.types.ObjectId

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
}

class GruntManagerImpl @Inject() (gruntDao: GruntDao, userDao: UserDao) extends GruntManager {

  /**
    * Inserts a new post and attaches it to a user.
    * @param newGrunt The new grunt to insert.
    * @return The new object if inserted correctly.
    */
  def insertNewGrunt(newGrunt: Grunt): Option[ObjectId] = {
    (gruntDao ++ newGrunt.copy(timestamp = CURRENT_TIMESTAMP)).map { id: ObjectId =>
      val users = userDao.findByHandles(MessageUtils.pullAtUsers(newGrunt.message))
      userDao.addGrunts(users.map(_.id.get) :+ newGrunt.userId, id)
      id
    }
  }
}