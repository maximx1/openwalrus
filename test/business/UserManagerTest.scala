package business

import java.io.File

import models.{Grunt, User}
import javax.inject.Inject
import org.mindrot.jbcrypt.BCrypt
import com.mongodb.casbah.Imports._

class UserManagerTest extends ManagerTestBase {
  
  @Inject val userManager: UserManager = injector.instanceOf[UserManager]
  
  "A User" should {
    "be able to login if they have a correct username/password combination for existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(user.handle) returning(Some(user.copy(password=BCrypt.hashpw(user.password, BCrypt.gensalt()))))
      userManager.login(user.handle, user.password) shouldBe Some(user.copy(password=""))
    }
    
    "fail to login if the user does not exist" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(*) returning(None)
      userManager.login(user.handle, user.password) shouldBe None
    }
    
    "fail to login if they have an incorrect username/password combination for existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(*) returning(None)
      userManager.login(user.handle, user.password) shouldBe None
    }
    
    "be able to be added and return true" in {
      val newId = new ObjectId
      (userDaoMock.++ _) expects(*) returning(Some(newId))
      userManager.createUser(createTestUser) shouldBe Some(newId)
    }
  }
  
  "A handle" should {
    "be shown to be in use if it matches an existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(user.handle) returning(Some(user))
      userManager.checkIfHandleInUse(user.handle) shouldBe true
    }
    
    "be shown to not be in use if it doesn't match an existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(*) returning(None)
      userManager.checkIfHandleInUse(user.handle) shouldBe false
    }
  }

  "User Profiles" should {
    "be found from Grunts" in {
      val userId1 = new ObjectId()
      val gruntTO1 = Grunt(Some(new ObjectId()), userId1, None, List.empty, List.empty,"", 0)
      val userId2 = new ObjectId()
      val gruntTO2 = Grunt(Some(new ObjectId()), userId2, None, List.empty, List.empty,"", 0)
      val list = gruntTO1 :: gruntTO2 :: Nil

      val user1 = User(Some(userId1), "", None, None, "", "", 0, true, true, None, None, List.empty, List.empty, List.empty, List.empty)
      val user2 = User(Some(userId2), "", None, None, "", "", 0, true, true, None, None, List.empty, List.empty, List.empty, List.empty)

      (userDaoMock.findByIds _) expects(list.map(_.userId)) returning(user1::user2::Nil)
      val results = userManager.getGruntProfiles(list)
      results.size shouldBe 2
      results(userId1.toString()).id shouldBe userId1.toString()
      results(userId2.toString()).id shouldBe userId2.toString()
    }
    
    "have its profile image updated and return the imageRef to show it's done" in {
      val userId = new ObjectId()
      val imageRef = new ObjectId()
      (userDaoMock.updateProfileImage _) expects(userId, imageRef) returning(Some(imageRef))
      userManager.updateProfileImage(userId, imageRef) shouldBe Some(imageRef)
    }
    
    "not have its profile image updated and return None to show it" in {
      val userId = new ObjectId()
      val imageRef = new ObjectId()
      (userDaoMock.updateProfileImage _) expects(userId, imageRef) returning(None)
      userManager.updateProfileImage(userId, imageRef) shouldBe None
    }
  }

  "An image" should {
    "be inserted into the database without creating a thumbnail" in {
      val file = new File("tmp.txt")
      val fileName = "tmp.txt"
      val expectedId = new ObjectId()
      (fileDaoMock.store (_: File, _: String)) expects(file, fileName) returning(Some(expectedId))
      userManager.insertPhoto(Some((fileName, file)), false) shouldBe Some((Some(expectedId), None))
    }

//    "be inserted into the database and create a thumbnail" in {
//      val file = new File("tmp.txt")
//      val fileName = "tmp.txt"
//      val expectedId = new ObjectId()
//      val expectedThumbId = new ObjectId()
//      (fileDaoMock.store (_: File, _: String)) expects(file, fileName) returning(Some(expectedId))
//      userManager.insertPhoto(Some((fileName, file)), false) shouldBe Some((Some(expectedId), None))
//
//    }
  }
  
  def createTestUser = User(None, "timmay", Some("test@sample.com"), None,"samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, None, List.empty, List.empty, List.empty, List.empty)
}