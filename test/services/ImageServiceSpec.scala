package services

import org.scalatestplus.play.PlaySpec

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO

class ImageServiceSpec extends PlaySpec {

  private def png(w: Int, h: Int): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out)
    out.toByteArray
  }

  "typeForBytes" should {
    "recognise a PNG by its contents" in {
      ImageService.typeForBytes(png(4, 4)) mustBe Some("image/png")
    }
    "refuse anything else, an SVG included" in {
      ImageService.typeForBytes("<svg onload=alert(1)></svg>".getBytes) mustBe None
    }
  }

  "thumbnail" should {
    "be a square of ThumbSize" in {
      val thumb = ImageIO.read(new ByteArrayInputStream(ImageService.thumbnail(ImageIO.read(new ByteArrayInputStream(png(400, 100))))))
      (thumb.getWidth, thumb.getHeight) mustBe (ImageService.ThumbSize, ImageService.ThumbSize)
    }
  }
}
