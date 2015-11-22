package core.utils

import org.jsoup._
import org.jsoup.safety.Whitelist
import org.jsoup.nodes.Document

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
  
  /**
   * Converts a chunk of html into plain text while preserving the newlines. This helps with markdown.
   */
  def htmlToTextWithNewLines(html: String): String = {
    val doc = Jsoup.parse(html)
    doc.outputSettings().prettyPrint(false)
    doc.select("br").append("\\n")
    doc.select("p").prepend("\\n\\n")
    doc.select("div").prepend("\\n\\n")
    val preStr = doc.html().replaceAll("\\\\n", "\n")
    Jsoup.clean(preStr, "", Whitelist.none, new Document.OutputSettings().prettyPrint(false))
  }
}
