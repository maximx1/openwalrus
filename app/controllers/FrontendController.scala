package controllers

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Environment}
import play.api.http.{FileMimeTypes, HttpEntity}
import play.api.mvc.*

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves the web client that Vite builds into public/. Any path that isn't the API or a
 * file of the client gets index.html, and the client's router takes it from there.
 *
 * The hashed bundles under /assets go through Play's Assets (conf/routes) and are cached
 * for good. The files at the top (index.html, the icons) are served from here instead,
 * with an ETag made from their contents and no caching without asking first. Play's own
 * ETag for them comes from the file's date, which the packager sets to the same day on
 * every build: browsers would keep an old index.html that names bundles that are gone,
 * and show a blank page after an update.
 *
 * /embed/:id is the one page other sites may put in a frame (embed.html, a small page of
 * its own showing one grunt). Everything else says frame-ancestors 'none'.
 */
@Singleton
class FrontendController @Inject() (cc: ControllerComponents, env: Environment, mimeTypes: FileMimeTypes, config: Configuration)
    extends AbstractController(cc) {

  private case class TopFile(bytes: Array[Byte], etag: String, contentType: String)

  private val files = new ConcurrentHashMap[String, Option[TopFile]]()

  def index: Action[AnyContent] = route("")

  def route(path: String): Action[AnyContent] = Action { request =>
    // Handles never contain a dot, so a dotted name can only be one of the files.
    serve(request, path.contains("."), if (path.contains(".")) path else "index.html")
  }

  /** The route skips the CSP filter (+nocsp), so it sets the app's policy itself, open to framing. */
  def embed(id: String): Action[AnyContent] = Action { request =>
    serve(request, isFile = false, "embed.html").withHeaders("Content-Security-Policy" -> embedPolicy)
  }

  private val embedPolicy =
    (config.get[Map[String, String]]("play.filters.csp.directives") + ("frame-ancestors" -> "*"))
      .map((name, value) => s"$name $value").mkString("; ")

  private def serve(request: RequestHeader, isFile: Boolean, name: String): Result =
    load(name) match {
      case None if isFile => NotFound
      case None => NotFound("The web client has not been built. Run `npm run build` in web/, or use the Vite dev server (see README.md).")
      case Some(f) if request.headers.get(IF_NONE_MATCH).contains(f.etag) => NotModified.withHeaders(ETAG -> f.etag, CACHE_CONTROL -> "no-cache")
      case Some(f) => Ok.sendEntity(HttpEntity.Strict(org.apache.pekko.util.ByteString(f.bytes), Some(f.contentType)))
          .withHeaders(ETAG -> f.etag, CACHE_CONTROL -> "no-cache")
    }

  /** Read once; they can't change while the app runs. */
  private def load(name: String): Option[TopFile] =
    files.computeIfAbsent(name, n =>
      env.resourceAsStream(s"public/$n").map { in =>
        val bytes = try in.readAllBytes() finally in.close()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).take(16).map("%02x".format(_)).mkString
        val contentType = mimeTypes.forFileName(n).map(t => if (t.startsWith("text/")) s"$t; charset=utf-8" else t)
          .getOrElse(BINARY)
        TopFile(bytes, "\"" + hash + "\"", contentType)
      }
    )
}
