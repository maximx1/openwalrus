package controllers

import javax.inject.Inject
import business.GruntManager
import models.Grunt
import models.json.JsonPayloads._
import org.bson.types.ObjectId
import models.UserTO
import models.GruntTO

class ApplicationAPI @Inject() (gruntManager: GruntManager) extends ApiControllerBase {
  
  val userNotLoggedIn = BasicResponse(Some("failed"), Some("User not logged in"))
  val unspecifiedError = BasicResponse(Some("failed"), Some("There was an unspecified error"))
  val couldNotFindId = BasicResponse(Some("failed"), Some("Could not find the id specified"))
  
  def postGrunt = JsonAction[NewGrunt] { implicit request =>
    request.session.get("userHandle").map { handle =>
      val newGrunt = Grunt(None, new ObjectId(request.session.get("userId").get), None, List.empty, List.empty, request.jsonData.message, 0)
      val newId: Option[ObjectId] = gruntManager.insertNewGrunt(newGrunt)
      println(newId)
      newId.map(id => Ok(BasicResponse(Some("ok"), Some(id.toString)) asJson)).getOrElse(Ok(unspecifiedError asJson))
    }.getOrElse(Ok(userNotLoggedIn asJson))
  }
  
  def retrieveSingleGrunt = JsonAction[SingleGruntRequest] { implicit request =>
    request.session.get("userHandle").map { handle =>
      ObjectId.isValid(request.jsonData.id) match {
        case true => {
          gruntManager.getGruntById(new ObjectId(request.jsonData.id)).map{case (gruntTO: GruntTO, userTO: UserTO) =>
            val htmlResponse = views.html.partials.grunt(gruntTO, Map(userTO.id.toString() -> userTO)).body
            println(htmlResponse)
            Ok(BasicResponse(Some("ok"), Some(htmlResponse)) asJson)
          }.getOrElse(Ok(couldNotFindId asJson))
        }
        case false => {
          Ok(couldNotFindId asJson)
        }
      }
    }.getOrElse(Ok(userNotLoggedIn asJson))
  }
}
