import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter
import com.mongodb.casbah.Imports._
import play.api.Play
import play.api.Application
import walrath.technology.openwalrus.daos.{MongoDaoBase, PersonDao}
import walrath.technology.openwalrus.model.tos.Person

class InsertPersonTest extends MongoTestBase with BeforeAndAfter {
  before { startMongoServer }
  after { stopMongoServer }

  "A connection to Mongo" should {
    "be made and a document should be able to be added" in {
      val personDao = new PersonDao()
      val person = Person(None, "Testy", "Testerson")
      personDao.storeUser(person)
      println(Person.fromMongoObjectList(personDao.findAll.toList))
      assert(personDao.count === 1)
    }
    
    "be able to insert more than one document and be read" in {
      val personDao = new PersonDao()
      val person1 = Person(None, "Testy", "Testerson")
      val person2 = Person(None, "Westy", "Westerson")
      personDao.storeUser(person1)
      personDao.storeUser(person2)
      println(Person.fromMongoObjectList(personDao.findAll.toList))
      assert(personDao.count === 2)
    }
  }
}