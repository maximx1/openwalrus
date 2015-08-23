package walrath.technology.openwalrus.business

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import walrath.technology.openwalrus.daos.UserDao
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec }
import play.api.Mode

trait ManagerTestBase extends WordSpec with BeforeAndAfter with Matchers with MockFactory {
  val userDaoMock = mock[UserDao]
  implicit lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[UserDao].toInstance(userDaoMock))
    .in(Mode.Test)
    .build
}