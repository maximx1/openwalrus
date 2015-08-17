package walrath.technology.openwalrus.daos

import com.github.simplyscala.MongodProps
import com.github.simplyscala.MongoEmbedDatabase
import scala.util.Try
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import com.mongodb.casbah.Imports._

trait MongoTestBase extends PlaySpec with OneAppPerSuite with MongoEmbedDatabase with MongoDaoBase {
    val testPort = 27017
    implicit override lazy val app: FakeApplication = FakeApplication(
      additionalConfiguration = Map(
        mongodbNameProp -> "testDB",
        mongodbURI -> ("mongodb://localhost:" + testPort.toString)
      )
    )
    protected var mongoInstance: MongodProps = null
    protected def startMongoServer = Try { mongoInstance = mongoStart(testPort) }
    protected def stopMongoServer = mongoStop(mongoInstance)
}