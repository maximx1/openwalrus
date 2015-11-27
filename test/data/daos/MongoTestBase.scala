package data.daos

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.scalatestplus.play._
import play.api.test._

import scala.util.Try

trait MongoTestBase extends PlaySpec with OneAppPerSuite with MongoEmbedDatabase with MongoDaoBaseConnectionHandler {
    val testPort = 27100
    
    implicit override lazy val app: FakeApplication = FakeApplication(
      additionalConfiguration = Map(
        mongodbNameProp -> "testDB",
        MongoDaoBaseConnectionHandler.mongodbURI -> ("mongodb://localhost:" + testPort.toString)
      )
    )
    
    protected var mongoInstance: MongodProps = null
    protected def startMongoServer = Try { mongoInstance = mongoStart(testPort) }
    protected def stopMongoServer = mongoStop(mongoInstance)
}