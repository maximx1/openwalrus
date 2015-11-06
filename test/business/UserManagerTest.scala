package business

import walrath.technology.openwalrus.model.tos.User
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
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
      (userDaoMock.++ _) expects(*) returning(Some(new ObjectId))
      userManager.createUser(createTestUser) shouldBe true
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
  
  def createTestUser = User(None, "timmay", Some("test@sample.com"), None,"samplePass", "Testy Testerson", System.currentTimeMillis(), true, true)
}