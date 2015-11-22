package business

import javax.inject.Inject

import models.{User, Grunt}
import org.bson.types.ObjectId

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
  }

  val user1 = User(Some(new ObjectId()), "timmay", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, List.empty, List.empty, List.empty, List.empty)
  val user2 = User(Some(new ObjectId()), "yammit", Some("test@sample.com"), None, "samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, None, List.empty, List.empty, List.empty, List.empty)
  def createTestGrunt = Grunt(None, user1.id.get, None, List.empty, List.empty, "sample message", 0)
}
