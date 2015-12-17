package data.daos

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.scalatestplus.play._
import play.api.test._
import scala.util.Try
import org.scalatest.Suites

trait MongoTestBase extends PlaySpec with MongoEmbedDatabase with ConfiguredApp {
  protected var mongoInstance: MongodProps = null
  protected def startMongoServer = Try { mongoInstance = mongoStart(MongoTestBase.testPort) }
  protected def stopMongoServer = mongoStop(mongoInstance)
}

object MongoTestBase extends MongoDaoBaseConnectionHandler{
  val testPort = 27100
}