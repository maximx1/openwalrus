package walrath.technology.openwalrus.business

import javax.inject.Inject
import walrath.technology.openwalrus.model.tos.User

class UserManagerTest @Inject()(userManager: UserManager) extends ManagerTestBase {
  
  "A User" should {
    "be able to login if they have a correct username/password combination for existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(user.handle) returning(Some(user))
      userManager.login(user.handle, user.password) shouldBe(user)
    }
    
    "fail to login if the user does not exist" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(*) returning(None)
      userManager.login(user.handle, user.password) shouldBe(None)
    }
    
    "fail to login if they have an incorrect username/password combination for existing user" in {
      val user = createTestUser
      (userDaoMock.findByHandle _) expects(*) returning(None)
      userManager.login(user.handle, user.password) shouldBe(None)
    }
  }
  
  def createTestUser = User(None, "timmay","test@sample.com","samplePass", "Testy", "Testerson", System.currentTimeMillis(), true)
}