package filters

import javax.inject.{Inject, Singleton}

import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
 * When the app is served over https (APP_URL starts with https, see OpenwalrusLoader), tells
 * browsers to use only https for a year, so a network can't quietly downgrade a visit.
 */
@Singleton
class StrictTransportSecurity @Inject() (config: Configuration)(using val mat: Materializer, ec: ExecutionContext) extends Filter {
  private val https = config.get[Boolean]("play.http.session.secure")

  def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] =
    if (https) next(request).map(_.withHeaders("Strict-Transport-Security" -> "max-age=31536000"))
    else next(request)
}
