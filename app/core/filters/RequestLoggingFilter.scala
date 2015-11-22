package core.filters

import java.util.UUID

import javax.inject.Singleton
import core.localization.Constants
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future

/**
  * Filter that logs the data of a request.
  */
@Singleton
class RequestLoggingFilter extends Filter {

  /**
    * Logs the data of a request and appends a correlation id to the header.
    * @param nextFilter The next filter in the chain.
    * @param requestHeader The request header.
    * @return The overall result
    */
  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val correlationId = UUID.randomUUID.toString
    logAllTheThings(requestHeader, correlationId)
    nextFilter(requestHeader.copy(headers = wrapHeaderWithCorrelationId(correlationId, requestHeader)))
  }

  /**
    * Creates a new header object by appending the data of the old one with a correlation id.
    * @param correlationId The generated id of the request.
    * @param requestHeader The original request header.
    * @return New header with a correlation id.
    */
  private def wrapHeaderWithCorrelationId(correlationId: String, requestHeader: RequestHeader): Headers =
    requestHeader.headers.get(Constants.correlationIdHeader).map(x => requestHeader.headers).getOrElse(
      new Headers((requestHeader.headers.toSimpleMap + (Constants.correlationIdHeader -> correlationId)).toList)
    )

  /**
    * Organized logger block.
    * @param request The request header.
    * @param correlationId The request id
    */
  private def logAllTheThings(request: RequestHeader, correlationId: String) = {
    val requestPacket: StringBuilder = new StringBuilder("\n==========================================")
    requestPacket.append("\nApplication: " + Constants.applicationName)
    requestPacket.append("\nRequest id: " + correlationId)
    requestPacket.append("\nRoute: " + request.method.toUpperCase + " " + request.uri)
    requestPacket.append(s"\nConnecting IP: ${request.remoteAddress}")
    requestPacket.append("\n<---- Request header ---->")
    request.headers.keys.foreach{x => requestPacket.append(s"\n$x: ${request.headers.getAll(x).mkString(" <> ")}")}
    requestPacket.append("\n<----   End header   ---->")
    requestPacket.append("\n==========================================")
    Logger.info(requestPacket.toString)
  }
}