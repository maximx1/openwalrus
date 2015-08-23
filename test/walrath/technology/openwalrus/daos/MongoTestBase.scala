package walrath.technology.openwalrus.daos

import com.github.simplyscala.MongodProps
import com.github.simplyscala.MongoEmbedDatabase
import scala.util.Try
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import com.mongodb.casbah.Imports._
import org.scalatest.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Mode

trait MongoTestBase extends PlaySpec with MongoEmbedDatabase with MongoDaoBase {
    val testPort = 27100
    
    implicit lazy val app = new GuiceApplicationBuilder().configure(Map(
        mongodbNameProp -> "testDB",
        mongodbURI -> ("mongodb://localhost:" + testPort.toString)
      )
    ).in(Mode.Test).build
    
    protected var mongoInstance: MongodProps = null
    protected def startMongoServer = Try { mongoInstance = mongoStart(testPort) }
    protected def stopMongoServer = mongoStop(mongoInstance)
}