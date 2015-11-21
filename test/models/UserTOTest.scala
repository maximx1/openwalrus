package models

import core.BaseTestSpec
import org.bson.types.ObjectId

class UserTOTest extends BaseTestSpec {
  "A UserTO" should {
    "be converted to a user object with a valid profileImage if the original is a valid ObjectId" in {
      val originalId = new ObjectId()
      val userTO = createTestUserTO(originalId.toString())
      val user = userTO.toUser
      user.profileImage shouldBe Some(originalId)
    }

    "be converted to a user object with a None profileImage if the original is noimage" in {
      val originalId = "noimage"
      val userTO = createTestUserTO(originalId)
      val user = userTO.toUser
      user.profileImage shouldBe None
    }

    "be converted to a user object with a None profileImage if the original is null" in {
      val originalId = null
      val userTO = createTestUserTO(originalId)
      val user = userTO.toUser
      user.profileImage shouldBe None
    }

    "be converted to a user object with a None profileImage if the original is an empty string" in {
      val originalId = ""
      val userTO = createTestUserTO(originalId)
      val user = userTO.toUser
      user.profileImage shouldBe None
    }
  }

  "A User" should {
    "be converted to a UserTO with a stringified ID if id exists" in {
      val originalId = new ObjectId()
      val user = createTestUser(Some(originalId))
      val userTO = UserTO.fromUser(user)
      userTO.profileImage shouldBe originalId.toString
    }

    "be converted to a UserTO with noimage if id doesn't exist" in {
      val user = createTestUser(None)
      val userTO = UserTO.fromUser(user)
      userTO.profileImage shouldBe "noimage"
    }
  }

  def createTestUser(imageId: Option[ObjectId]) = User(None, "timmay", Some("test@sample.com"), None,"samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, imageId, List.empty, List.empty, List.empty, List.empty)
  def createTestUserTO(imageId: String) = UserTO(None, "timmay", Some("test@sample.com"), None,"samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, imageId, List.empty, List.empty, List.empty, List.empty)
}
