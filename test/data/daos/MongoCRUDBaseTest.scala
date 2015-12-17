package data.daos

import com.google.inject.Inject
import com.mongodb.casbah.Imports._
import models.{BaseModel, BaseTOModel}
import org.scalatest.BeforeAndAfter
import play.api.Application
import org.scalatest.DoNotDiscover

@DoNotDiscover
class MongoCRUDBaseTest extends MongoTestBase with BeforeAndAfter {
  private var dao: TestDocumentDao = null
  
  before { 
    MongoDaoBaseConnectionHandler.closeConnection
    startMongoServer
    dao = new TestDocumentDao
  }
  after {
    MongoDaoBaseConnectionHandler.closeConnection
    stopMongoServer
  }
  
  "A TO" should {
    "be able to be created" in {
      val doc = createTestDoc
      dao.create(doc)
      val results = TestDocument.fromMongoObjectList(dao.findAll.toList)
      assert(results.size === 1)
      assert(results.filter(_.value1==doc.value1).size === 1)
    }
  }
  
  "Multiple TOs" should {
    "be able to be created" in {
      val doc1 = createTestDoc
      val doc2 = doc1.copy(value1="secondary_val")
      dao.create(doc1)
      dao.create(doc2)
      val results = TestDocument.fromMongoObjectList(dao.findAll.toList)
      assert(results.size === 2)
      assert(results.filter(_.value1==doc1.value1).size === 1)
      assert(results.filter(_.value1==doc2.value1).size === 1)
    }
  }
  
  "DB Count Operation" should {
    "be able to correctly count the number of documents in a database" in {
      val doc = createTestDoc
      (0 to 99).foreach(x => dao.create(doc))
      assert(dao.count === 100)
    }
  }
  
  "Read DB operations" should {
    "be able to retrieve all documents from collection" in {
      (0 to 99).foreach(x => dao.create(TestDocument(None, x.toString)))
      val results = TestDocument.fromMongoObjectList(dao.findAll.toList)
      val values = results.map(_.value1.toInt)
      assert(dao.count === 100)
      assert((0 to 99).filter(values.contains(_)).size === 100)
    }
  }
  
  def createTestDoc = TestDocument(None, "sample")
}

class TestDocumentDao @Inject() ()(implicit app: Application) extends MongoCRUDBase[TestDocument] {
  override val collName = "TestDocument"
}

case class TestDocument(
    id: Option[ObjectId],
    value1: String
  ) extends BaseTOModel {
  override def toMongoDBObject = MongoDBObject(
    "value1"->value1
  )
}

/**
 * @author maximx1
 */
object TestDocument extends BaseModel[TestDocument] {
  override def fromMongoObject(mongoObject: DBObject): TestDocument = TestDocument(
    Some(mongoObject.as[ObjectId]("_id")),
    mongoObject.as[String]("value1")
  )
}