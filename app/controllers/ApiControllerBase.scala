package controllers

import play.api.Logger
import play.api.mvc._
import org.json4s.jackson.Serialization
import org.json4s.{Formats, JValue, DefaultFormats, JInt, CustomSerializer, JNull}
import com.github.tototoshi.play2.json4s.jackson.Json4s
import org.joda.time.DateTime

trait ApiControllerBase extends Controller with Json4s with JsonImplicits with CorrelationIdKnowledgeable with JsonRequestDebugLoggerUtil {
  case class JsonActionRequest[T](val jsonData: T, request: Request[JValue]) extends WrappedRequest(request)

  def JsonAction[T](f: JsonActionRequest[T] => Result)(implicit formats: Formats, mf: Manifest[T]) = {
    Action(json) { request =>
      logJsonRequestBody(request)
      f(JsonActionRequest[T](request.body.extract[T], request))
    }
  }
}

trait JsonImplicits {
  protected implicit val format = DefaultFormats ++ List(DateTimeToMilliSerializer)
}

trait CorrelationIdKnowledgeable {
  protected def extractIdHeader[T]()(implicit request: Request[T]) = request.headers.get("CorrelationId")
}

trait JsonRequestDebugLoggerUtil {
  protected def logJsonRequestBody(request: Request[JValue])(implicit formats: Formats): Unit = {
    Logger.debug(Serialization.write(request.body))
  }
}

case object DateTimeToMilliSerializer extends CustomSerializer[DateTime](format =>
  (
    {
      case JInt(s) => new DateTime(s.toLong)
      case JNull => null
    },
    {
      case d: DateTime => JInt(d.getMillis)
    }
  )
)