package data.daos

import java.io.FileInputStream

import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfter

class FileGridFsDaoIntegrationTest extends MongoTestBase with GridFsBaseConnectionHandler with BeforeAndAfter {

  val testImage = "test/resources/testImage.png"
  private var gridFsDao: FileGridFsDao = null

  before {
    startMongoServer
    gridFsDao = new FileGridFsDao()
  }
  after { stopMongoServer }

  "A file" should {
    "be able to be inserted into the database" in {
      val newId = gridFsDao.store(getFile, "")
      gridFsDao.retrieve(newId.get) must not be (None)
    }

    "be able to be retrieved from the database" in {
      val newId = gridFsDao.store(getFile, "")
      gridFsDao.retrieve(newId.get) must not be (None)
    }

    "fail to be found when not in the database" in {
      gridFsDao.retrieve(new ObjectId()) mustBe None
    }

    "have it's name stored in the filename metadata" in {
      val newId = gridFsDao.store(getFile, "snicklefritz")
      gridFsDao.retrieve(newId.get).get.filename.get mustBe "snicklefritz"
    }
  }

  "A byte array" should {
    "be able to be inserted into the database like it's a file" in {
      val originalFile = getFile
      val byteArray = new Array[Byte](originalFile.length().toInt)
      val fInputStream = new FileInputStream(originalFile)
      fInputStream.read(byteArray)
      fInputStream.close()
      val newId = gridFsDao.store(byteArray, "")
      gridFsDao.retrieve(newId.get) must not be (None)
    }
  }

  def getFile = app.getFile(testImage)
}
