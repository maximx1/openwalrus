package models

import core.BaseTestSpec
import org.bson.types.ObjectId

class GruntTest extends BaseTestSpec {
  "A grunt" should {
    "Create a new object ID during conversion to MongoDBObject if the id is none" in {
      val grunt = createTestGrunt(None)
      val mdbObject = grunt.toMongoDBObject
      mdbObject.as[ObjectId]("_id").toString() should have length 24
    }

    "Not create a new object during conversion to MongoDBObject" in {
      val id = new ObjectId()
      val grunt = createTestGrunt(Some(id))
      val mdbObject = grunt.toMongoDBObject
      mdbObject.as[ObjectId]("_id") shouldBe id
    }

    "Be converted to a GruntTO" in {
      val userTO = createTestUserTO
      val grunt = createTestGrunt(Some(new ObjectId()))
      val gruntTO = GruntTO(grunt.userId, userTO.handle, userTO.fullName, grunt.message, grunt.timestamp)
      GruntTO.fromGrunt(grunt, userTO) shouldBe gruntTO.copy(message= "<p>" + gruntTO.message + "</p>\n")
    }
  }

  def createTestUserTO = UserTO(None, "timmay", Some("test@sample.com"), None,"samplePass", "Testy Testerson", System.currentTimeMillis(), true, true, "", List.empty, List.empty, List.empty, List.empty)
  def createTestGrunt(id: Option[ObjectId]) = Grunt(id, new ObjectId(), None, List.empty, List.empty, "My First Grunt", 0)
}
