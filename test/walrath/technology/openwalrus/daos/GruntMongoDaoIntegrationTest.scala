package walrath.technology.openwalrus.daos

import com.mongodb.casbah.Imports._

import org.scalatest.BeforeAndAfter
import walrath.technology.openwalrus.model.tos.Grunt

class GruntMongoDaoIntegrationTest extends MongoTestBase with BeforeAndAfter {
  
  private var gruntDao: GruntMongoDao = null
  
  before {
    startMongoServer
    gruntDao = new GruntMongoDao
  }
  after { stopMongoServer }
  
  "A grunt" should {
    "be created successfully and added to the db" in {
      val grunt = testGrunt
      gruntDao ++ grunt
      assert(gruntDao.count === 1)
    }
    
    "be created successfully, added to the db, and be accessible by it's id" in {
      val grunt = testGrunt
      val newId = gruntDao ++ grunt
      val actual: Option[Grunt] = gruntDao.findById(newId.get)
      
      assert(actual.get === grunt.copy(id=newId))
    }
    
    "be found by a user id" in {
      val grunt = testGrunt
      val newId = gruntDao ++ grunt
      val actual = gruntDao.findByUserId(grunt.userId).head
      assert(actual === grunt.copy(id=newId))
    }
  }
  
  "Multiple grunts" should {
    "be able to be pulled at once" in {
      val grunt = testGrunt
      val newIds = (1 to 10).map(x => gruntDao.++(grunt).get).toList
      val results: List[Grunt] = gruntDao.findByIds(newIds)
      assert(results.size === 10)
      assert(results.map(_.id.get).sorted === newIds.sorted)
    }
    
    "return an empty list when nothing is passed in" in {
      assert(gruntDao.findByIds(List.empty) === List.empty)
    }
    
    "return an empty list when the id is not found" in {
      assert(gruntDao.findByIds(new ObjectId :: Nil) === List.empty)
    }
    
    "be found by userId" in {
      val grunt = testGrunt
      val newId1 = gruntDao ++ grunt
      gruntDao ++ grunt.copy(userId=new ObjectId)
      val results = gruntDao.findByUserId(grunt.userId)
      assert(results.size === 1)
      assert(results.head === grunt.copy(id=newId1))
    }
  }
  
  def testGrunt: Grunt = Grunt(None, new ObjectId, None, List.empty, List.empty, "My first grunt", 0)
}