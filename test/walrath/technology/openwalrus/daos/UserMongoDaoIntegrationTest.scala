package walrath.technology.openwalrus.daos

import org.scalatest.BeforeAndAfter
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode

class UserMongoDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  var userDao: UserMongoDao = null
  
  before {
    startMongoServer
    userDao = new UserMongoDao()
  }
  after { stopMongoServer }

  "A User" should {
    "be made and a document should be able to be added to the db" in {
      val user = createTestUser
      userDao.create(user)
      assert(userDao.count === 1)
    }
    
    "found in db by looking for the handle if user exists" in {
      val user = createTestUser
      userDao.create(user)
      val results = userDao.findByHandle(user.handle)
      assert(results !== None)
      assert(results.get.copy(id=None) === user)
    }
    
    "not found in db by looking for the handle if user exists" in {
      val user = createTestUser
      val results = userDao.findByHandle(user.handle)
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