package services

import io.github.gitbucket.markedj.{Marked, Options}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, TextNode}
import org.jsoup.safety.Safelist

import scala.jdk.CollectionConverters.*

/** Turns what someone typed into a grunt into HTML that is safe to put on the page. */
object MessageRenderer {

  private val mentionPattern = """(?<![\w@])@(\w{1,30})""".r

  /** The handles @-mentioned in a message, without the @, each once. */
  def mentionedHandles(message: String): Seq[String] =
    mentionPattern.findAllMatchIn(message).map(_.group(1)).toSeq.distinctBy(_.toLowerCase)

  /**
   * Markdown to HTML, then cleaned down to formatting, links and images, then each
   * @handle outside a link or code turned into a link to that profile.
   */
  def toHtml(message: String): String = {
    val safelist = Safelist.basicWithImages().preserveRelativeLinks(true)
    val options = new Options()
    options.setSanitize(true) // raw HTML in the message is escaped, not passed through
    options.setSafelist(safelist)
    options.setBreaks(true)
    val markdown = Marked.marked(message, options)
    val body = Jsoup.parseBodyFragment(Jsoup.clean(markdown, "http://openwalrus.invalid/", safelist)).body()
    linkMentions(body)
    body.html()
  }

  private def linkMentions(root: Element): Unit = {
    val textNodes = root.select("*").asScala
      .filterNot(e => Set("a", "code", "pre").contains(e.normalName) || e.parents().asScala.exists(p => Set("a", "code", "pre").contains(p.normalName)))
      .flatMap(_.textNodes().asScala)
    textNodes.foreach { node =>
      val text = node.getWholeText
      val matches = mentionPattern.findAllMatchIn(text).toList
      if (matches.nonEmpty) {
        var pos = 0
        val parts = matches.flatMap { m =>
          val before = new TextNode(text.substring(pos, m.start))
          pos = m.end
          val link = new Element("a").attr("href", "/" + m.group(1)).text("@" + m.group(1))
          Seq(before, link)
        } :+ new TextNode(text.substring(pos))
        parts.foreach(node.before)
        node.remove()
      }
    }
  }
}
