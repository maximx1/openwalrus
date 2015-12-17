package data.daos

import org.scalatest.Suites
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

class MongoIntegrationTestSuiteRunner2 extends Suites(
  new FileGridFsDaoIntegrationTest,
  new GruntMongoDaoIntegrationTest,
  new ImageSetMongoDaoIntegrationTest,
  new MongoCRUDBaseTest,
  new UserMongoDaoIntegrationTest
) with OneAppPerSuite {
  implicit override lazy val app: FakeApplication = FakeApplication(
    additionalConfiguration = Map(
      MongoTestBase.mongodbNameProp -> "testDB",
      MongoDaoBaseConnectionHandler.mongodbURI -> ("mongodb://localhost:" + MongoTestBase.testPort.toString)
    )
  )
}