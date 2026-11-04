import play.api.inject.guice.{GuiceApplicationBuilder, GuiceApplicationLoader}
import play.api.{ApplicationLoader, Configuration}

import java.net.URI

/**
 * Play's usual loader, plus settings worked out from APP_URL, the address people open the
 * app at: only requests for that host are answered and, when the address is https, the
 * session cookie is Secure and named with the __Host- prefix (which browsers only accept
 * from this exact host, over https). Without APP_URL (development) the defaults stand.
 */
class OpenwalrusLoader extends GuiceApplicationLoader {
  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder =
    super.builder(context).configure(OpenwalrusLoader.fromAppUrl(sys.env.get("APP_URL").filter(_.trim.nonEmpty)))
}

object OpenwalrusLoader {
  def fromAppUrl(appUrl: Option[String]): Configuration = appUrl match {
    case None => Configuration.empty
    case Some(url) =>
      val uri = URI.create(url.trim)
      val host = Option(uri.getHost).getOrElse(sys.error(s"APP_URL must be a full address such as https://walrus.example.com, not '$url'"))
      val https = uri.getScheme == "https"
      Configuration(
        Seq(
          // localhost stays allowed for the container's own health check.
          "play.filters.hosts.allowed" -> Seq(host, "localhost", "127.0.0.1"),
          "play.http.session.secure" -> https
        ) ++ Option.when(https)("play.http.session.cookieName" -> "__Host-OPENWALRUS_SESSION")*
      )
  }
}
