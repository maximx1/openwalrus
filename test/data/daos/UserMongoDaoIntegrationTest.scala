package data.daos

import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfter
import models.User
import org.scalatest.DoNotDiscover

@DoNotDiscover
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
    
    "be able to have their banner image updated" in {
      val user1 = createTestUser.copy(bannerImage=Some(new ObjectId))
      val userId1 = userDao ++ user1
      val newBannerImage = new ObjectId
      userDao.updateBannerImage(userId1.get, newBannerImage)      
      val result1 = userDao.findById(userId1.get)
      result1.get.bannerImage mustBe Some(newBannerImage)
    }
    
    "be able to have their banner image updated and not affect another user" in {
      val user1 = createTestUser.copy(bannerImage=Some(new ObjectId))
      val user2 = createTestUser.copy(bannerImage=Some(new ObjectId))
      val userId1 = userDao ++ user1
      val userId2 = userDao ++ user2
      val newBannerImage = new ObjectId
      userDao.updateBannerImage(userId1.get, newBannerImage)
      val result1 = userDao.findById(userId1.get)
      val result2 = userDao.findById(userId2.get)
      result1.get.bannerImage mustBe Some(newBannerImage)
      result2.get.bannerImage mustBe user2.bannerImage
    }
    
    "be able to update who they are following" in {
      val user1 = createTestUser
      val id = userDao ++ user1
      val followingId = new ObjectId
      userDao.addFollowing(id.get, followingId)
      val foundUser = userDao.findById(id.get).get
      foundUser.following must contain (followingId)
    }
    
    "be able to update who they are following without affecting another user" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val id1 = userDao ++ user1
      val id2 = userDao ++ user2
      val followingId = new ObjectId
      userDao.addFollowing(id1.get, followingId)
      val foundUser1 = userDao.findById(id1.get).get
      val foundUser2 = userDao.findById(id2.get).get
      foundUser1.following must contain (followingId)
      foundUser2.following must not contain (followingId)
    }
    
    "be able to remove someone that they are no longer following" in {
    	val followingId = new ObjectId
      val user1: User = (createTestUser).copy(following=List(followingId))
      val id = userDao ++ user1
      userDao.removeFollowing(id.get, followingId)
      val foundUser = userDao.findById(id.get).get
      foundUser.following must not contain (followingId)
    }
    
    "be able to remove someone that they are no longer following without affecting another user" in {
    	val followingId = new ObjectId
      val user1 = (createTestUser).copy(following=List(followingId))
      val user2 = (createTestUser).copy(following=List(followingId))
      val id1 = userDao ++ user1
      val id2 = userDao ++ user2
      userDao.removeFollowing(id1.get, followingId)
      val foundUser1 = userDao.findById(id1.get).get
      val foundUser2 = userDao.findById(id2.get).get
      foundUser1.following must not contain (followingId)
      foundUser2.following must contain (followingId)
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
  
  "All users" should {
    "be pulled from the database at once if only 1 user" in {
      val user1 = createTestUser
      val userId1 = userDao ++ user1
      val result = userDao.all()
      result.sortBy(_.id) mustBe List(user1.copy(id=userId1)).sortBy(_.id)
    }
    
    "be pulled from the database at once if multiple users" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val userId1 = userDao ++ user1
      val userId2 = userDao ++ user2
      val result = userDao.all()
      result.sortBy(_.id) mustBe List(user1.copy(id=userId1), user2.copy(id=userId2)).sortBy(_.id)
    }
  }
  
  "A profile image" should {
    "be able to have updated in a user profile" in {
      val newImageRef = new ObjectId()
      val id = userDao ++ createTestUser
      userDao.updateProfileImage(id.get, newImageRef)
      val foundUser = userDao.findById(id.get).get
      foundUser.profileImage.get mustBe newImageRef
    }
  }
  
  "A follower" should {
    "be able to be added to a user" in {
      val user1 = createTestUser
      val id = userDao ++ user1
      val followerId = new ObjectId
      userDao.addFollower(id.get, followerId)
      val foundUser = userDao.findById(id.get).get
      foundUser.followers must contain (followerId)
    }
    
    "be able to be added to a user without affecting another user" in {
      val user1 = createTestUser
      val user2 = createTestUser
      val id1 = userDao ++ user1
      val id2 = userDao ++ user2
      val followerId = new ObjectId
      userDao.addFollower(id1.get, followerId)
      val foundUser1 = userDao.findById(id1.get).get
      val foundUser2 = userDao.findById(id2.get).get
      foundUser1.followers must contain (followerId)
      foundUser2.followers must not contain (followerId)
    }
    
    "be able to be removed from a user" in {
    	val followerId = new ObjectId
      val user1: User = (createTestUser).copy(followers=List(followerId))
      val id = userDao ++ user1
      userDao.removeFollower(id.get, followerId)
      val foundUser = userDao.findById(id.get).get
      foundUser.followers must not contain (followerId)
    }
    
    "be able to be removed from a user without affecting another user" in {
    	val followerId = new ObjectId
      val user1: User = (createTestUser).copy(followers=List(followerId))
      val user2: User = (createTestUser).copy(followers=List(followerId))
      val id1 = userDao ++ user1
      val id2 = userDao ++ user2
      userDao.removeFollower(id1.get, followerId)
      val foundUser1 = userDao.findById(id1.get).get
      val foundUser2 = userDao.findById(id2.get).get
      foundUser1.followers must not contain (followerId)
      foundUser2.followers must contain (followerId)
    }
  }
  
  def createTestUser = User(None, "timmay", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, None, List.empty, List.empty, List.empty, List.empty)
}