package walrath.technology.openwalrus.daos

import org.scalatest.BeforeAndAfter
import com.mongodb.casbah.Imports._
import walrath.technology.openwalrus.model.tos.User
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode
import javax.inject.Inject

class UserMongoDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  
  private var userDao: UserMongoDao = null
  
  before {
    startMongoServer
    userDao = new UserMongoDao
  }
  after { stopMongoServer }

  "A User" should {
    "be made and a document should be able to be added to the db" in {
      val user = createTestUser
      userDao ++ user
      assert(userDao.count === 1)
    }
    
    "should be inserted and have the id that was created upon insert" in {
      val user = createTestUser
      val newId = userDao ++ user
      assert(userDao.findByHandle(user.handle).get.id === newId)
    }
    
    "be found in db by looking for the handle if user exists" in {
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
    
    "be created successfully, added to the db, and be accessible by it's id" in {
      val user = createTestUser
      val newId = userDao ++ user
      val actual: Option[User] = userDao.findById(newId.get)
      
      assert(actual.get === user.copy(id=newId))
    }
  }
  
  "Multiple Users" should {
    "be made and documents should be able to be added to the db" in {
      val user1 = createTestUser
      val user2 = createTestUser.copy(fullName="Westy Westerson")
      userDao.create(user1)
      userDao.create(user2)
      assert(userDao.count === 2)
    }
    
    "be able to be pulled at once" in {
      val grunt = createTestUser
      val newIds = (1 to 10).map(x => userDao.++(grunt).get).toList
      val results: List[User] = userDao.findByIds(newIds)
      assert(results.size === 10)
      assert(results.map(_.id.get).sorted === newIds.sorted)
    }
  }
  
  def createTestUser = User(None, "timmay", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true)
}