package walrath.technology.openwalrus.daos

import org.scalatest.BeforeAndAfter
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User

class UserDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  var userDao: UserDao = null
  before { 
    startMongoServer
    userDao = new UserDao()
  }
  after { stopMongoServer }

  "A User" should {
    "be made and a document should be able to be added to the db" in {
      val user = createTestUser
      userDao.create(user)
      assert(userDao.count === 1)
    }
    
    "not be stopped from logging in with valid credentials" in {
      val user = createTestUser
      userDao.create(user)
      val results = userDao.login(user.handle, user.password)
      assert(results !== None)
    }
    
    "be stopped from logging in with invalid credentials" in {
      val user = createTestUser
      userDao.create(user)
      val results = userDao.login(user.handle, "asdf")
      assert(results === None)
    }
  }
  
  "Multiple Users" should {
    "be made and documents should be able to be added to the db" in {
      val user1 = createTestUser
      val user2 = createTestUser.copy(firstName="Westy", lastName="Westerson")
      userDao.create(user1)
      userDao.create(user2)
      assert(userDao.count === 2)
    }
  }
  
  def createTestUser = User(None, "timmay","test@sample.com","samplePass", "Testy", "Testerson", System.currentTimeMillis(), true)
}