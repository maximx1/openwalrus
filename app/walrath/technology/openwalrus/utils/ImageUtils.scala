package walrath.technology.openwalrus.utils

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File}
import javax.imageio.ImageIO

import scala.util.Try

/**
 * Utilities surrounding basic image tasks.
 * @author maximx1
 */
object ImageUtils {

  private val imageTypes = Map(
      "png" -> "image/png",
      "jpeg" -> "image/jpeg",
      "jpg" -> "image/jpeg",
      "gif" -> "image/gif",
      "svg" -> "image/svg"
  )

  /**
   * Determines the type of the image and returns is webtype.
   * @param filename The string to check the end of.
   * @return The webtype.
   */
  def determineWebType(filename: String): Option[String] = imageTypes.keys.filter(filename.endsWith).headOption.map(imageTypes(_))

  /**
   * Center crops and image and resizes it to something manageable.
   * @param file The original file which will remain untouched.
   * @return A new virtual file of the cropped image.
   */
  def imageToThumb(file: File): Try[Array[Byte]] = Try {
    val bufferedImage = ImageIO.read(file)
    val thumbImage = resize(centerSquareCrop(bufferedImage), 256, 256)
    val byteArrayOutputStream = new ByteArrayOutputStream()
    ImageIO.write(thumbImage, "png", byteArrayOutputStream)
    byteArrayOutputStream.flush()
    val thumb: Array[Byte] = byteArrayOutputStream.toByteArray
    byteArrayOutputStream.close()
    thumb
  }

  /**
   * Crops an image into a square with centering on center.
   * @param bufferedImage The image to square.
   * @return The cropped image.
   */
  def centerSquareCrop(bufferedImage: BufferedImage): BufferedImage = {
    val x: Int = bufferedImage.getWidth
    val y: Int = bufferedImage.getHeight
    val squareSide: Int = math.min(x, y)
    val xPrime: Int = (x-squareSide) / 2
    val yPrime: Int = (y-squareSide) / 2
    return bufferedImage.getSubimage(xPrime, yPrime, squareSide, squareSide)
  }

  /**
   * Resizes an image according to the dimensions passed in.
   * @param bufferedImage The Image to resize.
   * @param x The new width.
   * @param y The new Height
   * @return The resized image.
   */
  def resize(bufferedImage: BufferedImage, x: Int, y: Int): BufferedImage = {
    val resized = bufferedImage.getScaledInstance(x, y, Image.SCALE_SMOOTH)
    val reBuffered = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB)
    val graphics = reBuffered.createGraphics()
    graphics.drawImage(resized,0, 0, null)
    graphics.dispose()
    return reBuffered
  }
}