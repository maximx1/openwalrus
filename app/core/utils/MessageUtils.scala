package core.utils

/**
  * Utilities to handle operations on messages.
  */
object MessageUtils {

  private val atUserPattern = """@\w+""".r

  /**
    * Finds any user handles referenced by @<someString>
    * @param message The string to check.
    * @return The list of found @ referenced users.
    */
  def pullAtUsers(message: String): List[String] = atUserPattern.findAllMatchIn(message).map(_.toString().substring(1)).toList
}
