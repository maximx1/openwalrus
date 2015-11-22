package models.json

import controllers.JsonImplicits
import org.json4s.Extraction

/**
  * Base trait to define Json utilities on a model.
  */
trait Jsonify extends JsonImplicits {
  def asJson = Extraction.decompose(this)
}
