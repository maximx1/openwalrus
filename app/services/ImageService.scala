package services

import javax.inject.{Inject, Singleton}

import models.StoredImage
import repositories.ImageRepository

import java.awt.image.BufferedImage
import java.awt.{Image, RenderingHints}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** Accepts uploaded pictures, makes the square thumbnail, and hands them to storage. */
@Singleton
class ImageService @Inject() (images: ImageRepository)(using ec: ExecutionContext) {
  import ImageService.*

  /** Left is a message for the person uploading. */
  def store(file: Path, fileName: String): Future[Either[String, String]] = {
    val bytes = Files.readAllBytes(file)
    typeForBytes(bytes) match {
      case None => Future.successful(Left("That doesn't look like a PNG, JPEG or GIF image."))
      case Some(contentType) =>
        Try(ImageIO.read(new ByteArrayInputStream(bytes))).toOption.flatMap(Option(_)) match {
          case None => Future.successful(Left("That image could not be read."))
          case Some(image) =>
            val thumb = StoredImage(thumbnail(image), "image/png")
            images.store(StoredImage(bytes, contentType), thumb, fileName).map(Right(_))
        }
    }
  }

  def find(id: String, thumb: Boolean): Future[Option[StoredImage]] = images.find(id, thumb)
}

object ImageService {
  val ThumbSize = 256

  /** Decides by the file's first bytes, not its name, so nothing else gets served as an image. */
  def typeForBytes(b: Array[Byte]): Option[String] =
    if (b.length > 8 && b(0) == 0x89.toByte && b(1) == 'P' && b(2) == 'N' && b(3) == 'G') Some("image/png")
    else if (b.length > 3 && b(0) == 0xFF.toByte && b(1) == 0xD8.toByte && b(2) == 0xFF.toByte) Some("image/jpeg")
    else if (b.length > 6 && new String(b.take(4), "US-ASCII") == "GIF8") Some("image/gif")
    else None

  /** For images stored by the 2015 version, which only kept the type alongside the name. */
  def typeForName(name: String): String = Option(name).map(_.toLowerCase) match {
    case Some(n) if n.endsWith(".jpg") || n.endsWith(".jpeg") => "image/jpeg"
    case Some(n) if n.endsWith(".gif") => "image/gif"
    case _ => "image/png"
  }

  /** Centre-cropped to a square and scaled to ThumbSize, as PNG. */
  def thumbnail(image: BufferedImage): Array[Byte] = {
    val side = math.min(image.getWidth, image.getHeight)
    val square = image.getSubimage((image.getWidth - side) / 2, (image.getHeight - side) / 2, side, side)
    val out = new BufferedImage(ThumbSize, ThumbSize, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.drawImage(square.getScaledInstance(ThumbSize, ThumbSize, Image.SCALE_SMOOTH), 0, 0, null)
    g.dispose()
    val bytes = new ByteArrayOutputStream()
    ImageIO.write(out, "png", bytes)
    bytes.toByteArray
  }
}
