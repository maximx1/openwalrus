package business

import core.BaseTestSpec
import data.daos.{GruntDao, UserDao, FileDao}
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder

trait ManagerTestBase extends BaseTestSpec {
  val userDaoMock = mock[UserDao]
  val fileDaoMock = mock[FileDao]
  val gruntDaoMock = mock[GruntDao]
    
  val injector = new GuiceInjectorBuilder()
    .overrides(bind[UserDao].toInstance(userDaoMock))
    .overrides(bind[FileDao].toInstance(fileDaoMock))
    .overrides(bind[GruntDao].toInstance(gruntDaoMock))
    .injector
}