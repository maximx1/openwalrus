package controllers

import org.json4s._

class ApplicationAPI extends ApiControllerBase {
  def postGrunt = JsonAction[NewGrunt] { implicit request =>
    request.session.get("userHandle").map { handle =>
      Ok(BasicResponse(Some("ok"), Some(handle + " - " + request.jsonData.message)) asJson)
    }.getOrElse(Ok(BasicResponse(Some("failed"), Some("User not logged in")) asJson))
  }
}

case class BasicResponse(status: Option[String], content: Option[Any]) extends Jsonify

case class NewGrunt(message: String) extends Jsonify

trait Jsonify extends JsonImplicits {
  def asJson = Extraction.decompose(this)
}