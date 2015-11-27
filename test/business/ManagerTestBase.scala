package business

import core.BaseTestSpec
import data.daos.{GruntDao, UserDao, FileDao}
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import data.daos.ImageSetDao

trait ManagerTestBase extends BaseTestSpec {
  val userDaoMock = mock[UserDao]
  val fileDaoMock = mock[FileDao]
  val gruntDaoMock = mock[GruntDao]
  val imageSetDaoMock = mock[ImageSetDao]
    
  val injector = new GuiceInjectorBuilder()
    .overrides(bind[UserDao].toInstance(userDaoMock))
    .overrides(bind[FileDao].toInstance(fileDaoMock))
    .overrides(bind[GruntDao].toInstance(gruntDaoMock))
    .overrides(bind[ImageSetDao].toInstance(imageSetDaoMock))
    .injector
}