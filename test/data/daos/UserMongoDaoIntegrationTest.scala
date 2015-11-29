package data.daos

import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfter
import models.User

class UserMongoDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  
  private var userDao: UserMongoDao = null
  
  before {
    MongoDaoBaseConnectionHandler.closeConnection
    startMongoServer
    userDao = new UserMongoDao
  }
  after { 
    MongoDaoBaseConnectionHandler.closeConnection
    stopMongoServer
  }

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

    "be found in db by looking for the handle if user exists with case insensitive" in {
      val user = createTestUser
      userDao.create(user.copy(handle = user.handle.toLowerCase))
      val results = userDao.findByHandle(user.handle.toUpperCase)
      assert(results !== None)
      assert(results.get.copy(id=None) === user.copy(handle = user.handle.toLowerCase))
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

    "be able to have a grunt assigned to them" in {
      val user1 = createTestUser
      val userId1 = userDao ++ user1
      val gruntId = new ObjectId()
      userDao.addGrunt(userId1.get, gruntId)
      val results = userDao.findById(userId1.get)
      results.get.grunts must contain(gruntId)
    }

    "be able to have a grunt assigned to them without affecting other users" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val userId1 = userDao ++ user1
      val userId2 = userDao ++ user2
      val gruntId = new ObjectId()
      userDao.addGrunt(userId1.get, gruntId)
      val result1 = userDao.findById(userId1.get)
      val result2 = userDao.findById(userId2.get)
      result1.get.grunts must contain(gruntId)
      result2.get.grunts must not contain(gruntId)
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
      val user = createTestUser
      val newIds = (1 to 10).map(x => userDao.++(user).get).toList
      val results: List[User] = userDao.findByIds(newIds)
      assert(results.size === 10)
      assert(results.map(_.id.get).sorted === newIds.sorted)
    }

    "be able to be pulled at once via the handle" in {
      val user = createTestUser
      (1 to 10).foreach(x => userDao.++(user.copy(handle = user.handle + x.toString)))
      val expected = (1 to 10).map(x => user.handle + x.toString).toList
      val results: List[User] = userDao.findByHandles(expected)
      results must have length 10
      results.map(_.handle).sorted mustBe expected.sorted
    }

    "be able to be pulled case insensitively" in {
      val user1 = createTestUser.copy(handle = "asdf")
      val user2 = createTestUser.copy(handle = "fdsa")
      userDao ++ user1
      userDao ++ user2
      val results = userDao.findByHandles(List(user1.handle.toUpperCase, user2.handle.toUpperCase))
      results must have length 2
      results.map(_.handle) must contain(user1.handle)
      results.map(_.handle) must contain(user2.handle)
    }

    "be able to have a grunt assigned to them" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val userId1 = userDao ++ user1
      val userId2 = userDao ++ user2
      val gruntId = new ObjectId()
      userDao.addGrunts(List(userId1.get, userId2.get), gruntId)
      val results = userDao.findByIds(List(userId1.get, userId2.get))
      results must have length 2
      results(0).grunts must contain(gruntId)
      results(1).grunts must contain(gruntId)
    }

    "be able to have a grunt assigned to them without affecting another user" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val user3 = createTestUser
      val userId1 = userDao ++ user1
      val userId2 = userDao ++ user2
      val userId3 = userDao ++ user3
      val gruntId = new ObjectId()
      userDao.addGrunts(List(userId1.get, userId2.get), gruntId)

      val withGruntsResults = userDao.findByIds(List(userId1.get, userId2.get))
      withGruntsResults must have length 2
      withGruntsResults(0).grunts must contain(gruntId)
      withGruntsResults(1).grunts must contain(gruntId)

      val withoutGruntsResults = userDao.findById(userId3.get).get
      withoutGruntsResults.grunts must have length 0
    }
  }
  
  "A profile image" should {
    "Be able to have updated in a user profile" in {
      val newImageRef = new ObjectId()
      val id = userDao ++ createTestUser
      userDao.updateProfileImage(id.get, newImageRef)
      val foundUser = userDao.findById(id.get).get
      foundUser.profileImage.get mustBe newImageRef
    }
  }
  
  def createTestUser = User(None, "timmay", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, None, List.empty, List.empty, List.empty, List.empty)
}