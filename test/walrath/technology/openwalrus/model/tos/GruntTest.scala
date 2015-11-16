package walrath.technology.openwalrus.model.tos

import org.bson.types.ObjectId
import walrath.technology.openwalrus.testing.BaseTestSpec

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
  }

  def createTestGrunt(id: Option[ObjectId]) = Grunt(id, new ObjectId(), None, List.empty, List.empty, "My First Grunt", 0)
}
