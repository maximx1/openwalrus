package walrath.technology.openwalrus.utils

/**
 * Utilities surrounding basic image tasks.
 * @author maximx1
 */
object ImageUtils {
  
  private val imageTypes = Map(
      "png" -> "image/png",
      "jpeg" -> "image/jpeg",
      "jpg" -> "image/jpeg",
      "gif" -> "image/gif"
  )

  /**
   * Determines the type of the image and returns is webtype.
   * @param filename The string to check the end of.
   * @return The webtype.
   */
  def determineWebType(filename: String): Option[String] = imageTypes.keys.filter(filename.endsWith).headOption.map(imageTypes(_))
}