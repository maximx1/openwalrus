package data.daos

import org.scalatest.BeforeAndAfter
import org.bson.types.ObjectId
import models.ImageSet

class ImageSetMongoDaoIntegrationTest  extends MongoTestBase with BeforeAndAfter {
  
  private var imageSetDao: ImageSetDao = null
  
  before {
    MongoDaoBaseConnectionHandler.closeConnection
    startMongoServer
    imageSetDao = new ImageSetMongoDao
  }
  
  after { 
    MongoDaoBaseConnectionHandler.closeConnection
    stopMongoServer
  }
  
  "An ImageSet" should {
    "be able to be stored" in {
      val imageSet = ImageSet(None, new ObjectId, Some(new ObjectId))
      val newId = imageSetDao ++ imageSet
      imageSetDao.findById(newId.get).get mustBe imageSet.copy(newId)
    }
    
    "be found by it's id" in {
      val imageSet = ImageSet(None, new ObjectId, Some(new ObjectId))
      val newId = imageSetDao ++ imageSet
      imageSetDao.findById(newId.get).get mustBe imageSet.copy(newId)
    }
    
    "not be found by it's id if it doesn't exist" in {
      imageSetDao.findById(new ObjectId) mustBe None
    }
  }
  
  "Multiple ImageSets" should {
    "be able to be found" in {
      val imageSet1 = ImageSet(None, new ObjectId, Some(new ObjectId))
      val imageSet2 = ImageSet(None, new ObjectId, Some(new ObjectId))
      val newId1 = imageSetDao ++ imageSet1
      val newId2 = imageSetDao ++ imageSet2
      imageSetDao.findByIds(List(newId1.get, newId2.get)).sortBy(_.original.toString) mustBe List(imageSet1.copy(newId1), imageSet2.copy(newId2)).sortBy(_.original.toString)
    }
  }
}