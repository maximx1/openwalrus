package walrath.technology.openwalrus.daos

import org.scalatest.BeforeAndAfter
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User

class UserDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  before { startMongoServer }
  after { stopMongoServer }

  "A User" should {
    "be made and a document should be able to be added to the db" in {
      val userDao = new UserDao()
      val person = createTestUser
      userDao.storeUser(person)
      println(User.fromMongoObjectList(userDao.findAll.toList))
      assert(userDao.count === 1)
    }
  }
  
  "Multiple Users" should {
    "be made and documents should be able to be added to the db" in {
      val userDao = new UserDao()
      val user1 = createTestUser
      val user2 = createTestUser.copy(firstName="Westy", lastName="Westerson")
      userDao.storeUser(user1)
      userDao.storeUser(user2)
      println(User.fromMongoObjectList(userDao.findAll.toList))
      assert(userDao.count === 2)
    }
  }
  
  def createTestUser = User(None, "timmay","test@sample.com","samplePass", "Testy", "Testerson", System.currentTimeMillis(), true)
}