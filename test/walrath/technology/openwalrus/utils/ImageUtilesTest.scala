package walrath.technology.openwalrus.utils

import walrath.technology.openwalrus.testing.BaseTestSpec
import ImageUtils.determineWebType


class ImageUtilesTest extends BaseTestSpec {
  "ImageUtils" should {
    "infer image web types from filename" in {
      determineWebType("sample.png") shouldBe Some("image/png")
      determineWebType("sample.jpeg") shouldBe Some("image/jpeg")
      determineWebType("sample.jpg") shouldBe Some("image/jpeg")
      determineWebType("sample.gif") shouldBe Some("image/gif")
    }

    "return None should it not be able to infer type" in {
      determineWebType("asdfasdfadsfadf") shouldBe None
    }
  }
}
