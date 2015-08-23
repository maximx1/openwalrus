package walrath.technology.openwalrus.business

import play.api.inject.bind
import walrath.technology.openwalrus.daos.UserDao
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec }
import play.api.Mode
import play.api.inject.guice.GuiceInjectorBuilder
import org.scalatest.OneInstancePerTest

trait ManagerTestBase extends WordSpec with BeforeAndAfter with Matchers with MockFactory with OneInstancePerTest {
  val userDaoMock = mock[UserDao]
    
  val injector = new GuiceInjectorBuilder()
    .overrides(bind[UserDao].toInstance(userDaoMock))
    .injector
}