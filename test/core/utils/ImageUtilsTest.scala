package core.utils

import javax.imageio.ImageIO

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test._
import ImageUtils._

class ImageUtilsTest extends PlaySpec with OneAppPerSuite {

  val testImage = "test/resources/testImage.png"
  implicit override lazy val app: FakeApplication = FakeApplication()

  "ImageUtils" should {
    "infer image web types from filename" in {
      determineWebType("sample.png") mustBe Some("image/png")
      determineWebType("sample.jpeg") mustBe Some("image/jpeg")
      determineWebType("sample.jpg") mustBe Some("image/jpeg")
      determineWebType("sample.gif") mustBe Some("image/gif")
    }

    "return None should it not be able to infer type" in {
      determineWebType("asdfasdfadsfadf") mustBe None
    }

    "convert an image into a thumbnail" in {
      imageToThumb(app.getFile(testImage)).get must not be empty
    }

    "be able to square an image from its center" in {
      val original = ImageIO.read(app.getFile(testImage))
      val centerCropped = centerSquareCrop(original)
      centerCropped.getWidth() mustBe 300
      centerCropped.getHeight() mustBe 300
    }

    "be able to resize an image" in {
      val original = ImageIO.read(app.getFile(testImage))
      val resized = resize(original, 200, 150)
      resized.getWidth() mustBe 200
      resized.getHeight() mustBe 150
    }
  }
}
