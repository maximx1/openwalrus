package business

import javax.inject.Inject
import models.{User, Grunt}
import org.bson.types.ObjectId
import models.UserTO
import models.GruntTO

class GruntManagerTest extends ManagerTestBase {

  @Inject val gruntManager: GruntManager = injector.instanceOf[GruntManager]

  "A grunt" should {
    "be able to be inserted to and have it's id added to a user" in {
      val newId = new ObjectId()
      val newGrunt = createTestGrunt
      (gruntDaoMock.++ _) expects(*) returning(Some(newId))
      (userDaoMock.findByHandles _) expects(List.empty[String]) returning(List.empty)
      (userDaoMock.addGrunts _) expects(List(newGrunt.userId), newId) returning(true)
      gruntManager.insertNewGrunt(newGrunt) shouldBe Some(newId)
    }
    
    "be found by its id along with it's userTO" in {
      val newId = new ObjectId()
      val newGrunt = createTestGrunt
      val userTO = UserTO.fromUser(user1)
      val gruntTO = GruntTO.fromGrunt(newGrunt, userTO)
      (gruntDaoMock.findById _) expects(newId) returning(Some(newGrunt))
      (userDaoMock.findById _) expects(newGrunt.userId) returning(Some(user1))
      gruntManager.getGruntById(newId) shouldBe Some(gruntTO, userTO)
    }
    
    "not be found if the userTO isn't found that goes along with it" in {
      val newId = new ObjectId()
      val newGrunt = createTestGrunt
      (gruntDaoMock.findById _) expects(newId) returning(Some(newGrunt))
      (userDaoMock.findById _) expects(newGrunt.userId) returning(None)
      gruntManager.getGruntById(newId) shouldBe None
    }
    
    "not be found if not in db" in {
      val newId = new ObjectId()
      (gruntDaoMock.findById _) expects(newId) returning(None)
      gruntManager.getGruntById(newId) shouldBe None
    }
  }

  val user1 = User(Some(new ObjectId()), "timmay", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, None, List.empty, List.empty, List.empty, List.empty)
  val user2 = User(Some(new ObjectId()), "yammit", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, None, List.empty, List.empty, List.empty, List.empty)
  def createTestGrunt = Grunt(None, user1.id.get, None, List.empty, List.empty, "sample message", 0)
}
