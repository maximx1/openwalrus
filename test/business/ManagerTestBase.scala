package business

import core.BaseTestSpec
import data.daos.UserDao
import play.api.inject.bind
import data.daos.FileDao
import play.api.inject.guice.GuiceInjectorBuilder

trait ManagerTestBase extends BaseTestSpec {
  val userDaoMock = mock[UserDao]
  val fileDaoMock = mock[FileDao]
    
  val injector = new GuiceInjectorBuilder()
    .overrides(bind[UserDao].toInstance(userDaoMock))
    .overrides(bind[FileDao].toInstance(fileDaoMock))
    .injector
}