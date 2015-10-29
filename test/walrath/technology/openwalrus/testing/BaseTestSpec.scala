package walrath.technology.openwalrus.testing

import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest


trait BaseTestSpec extends WordSpec with BeforeAndAfter with Matchers with MockFactory with OneInstancePerTest