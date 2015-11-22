package models.json

/**
  * Object containing JSON payloads for easy import.
  */
object JsonPayloads {
  case class BasicResponse(status: Option[String], content: Option[Any]) extends Jsonify
  case class NewGrunt(message: String) extends Jsonify
  case class SingleGruntRequest(id: String) extends Jsonify
}
