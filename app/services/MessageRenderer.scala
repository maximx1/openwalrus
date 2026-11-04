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

  /** Where an uploaded video is served; the composer links one in as an image. */
  private val uploadedVideo = """^/api/videos/[0-9a-f]{24}$""".r

  /**
   * Markdown to HTML, then cleaned down to formatting, links and images, then each
   * @handle outside a link or code turned into a link to that profile, and each "image"
   * that is one of the app's own videos turned into a player.
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
    showVideos(body)
    body.html()
  }

  private val uploadedMedia = """^/api/(images|videos)/([0-9a-f]{24})(?:#.*)?$""".r

  /** The app's own pictures and videos a message shows, in order, as ("image" or "video", id). */
  def media(message: String): Seq[(String, String)] =
    Jsoup.parseBodyFragment(toHtml(message)).select("img[src], video[src]").asScala.toSeq.flatMap { e =>
      uploadedMedia.findFirstMatchIn(e.attr("src")).map(m => (if (m.group(1) == "images") "image" else "video", m.group(2)))
    }.distinct

  // Built here, after cleaning, from nothing but the checked address, so no attribute of
  // the user's survives onto the element. "#t=0.1" makes Safari show the first frame.
  private def showVideos(root: Element): Unit =
    root.select("img").asScala.filter(img => uploadedVideo.matches(img.attr("src"))).foreach { img =>
      img.replaceWith(
        new Element("video")
          .attr("src", img.attr("src") + "#t=0.1")
          .attr("controls", true)
          .attr("playsinline", true)
          .attr("preload", "metadata")
      )
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
