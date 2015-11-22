package controllers

import javax.inject.Inject

import business.GruntManager
import models.Grunt
import models.json.JsonPayloads._
import org.bson.types.ObjectId

class ApplicationAPI @Inject() (gruntManager: GruntManager) extends ApiControllerBase {
  def postGrunt = JsonAction[NewGrunt] { implicit request =>
    request.session.get("userHandle").map { handle =>
      val newGrunt = Grunt(None, new ObjectId(request.session.get("userId").get), None, List.empty, List.empty, request.jsonData.message, 0)
      val newId: Option[ObjectId] = gruntManager.insertNewGrunt(newGrunt)
      Ok(BasicResponse(Some("ok"), Some(newId.get.toString)) asJson)
    }.getOrElse(Ok(BasicResponse(Some("failed"), Some("User not logged in")) asJson))
  }
}
