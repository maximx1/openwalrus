package core

import org.scalatestplus.play.OneAppPerSuite
import org.scalatest.Suites
import play.api.test.FakeApplication
import core.utils.ImageUtilsTest

class CommonAppEnabledTestSuiteRunner extends Suites(new ImageUtilsTest) with OneAppPerSuite {
  implicit override lazy val app: FakeApplication = FakeApplication()
}