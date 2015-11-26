package controllers

import play.api.mvc._
import javax.inject.Inject
import business.GruntManager
import models.Grunt
import models.json.JsonPayloads._
import org.bson.types.ObjectId
import models.UserTO
import models.GruntTO
import business.FileManager

class ApplicationAPI @Inject() (gruntManager: GruntManager, fileManager: FileManager) extends ApiControllerBase {
  
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
  
  def retrieveSingleGrunt = JsonAction[SingleIdRequest] { implicit request =>
    request.session.get("userHandle").map { handle =>
      ObjectId.isValid(request.jsonData.id) match {
        case true => {
          gruntManager.getGruntById(new ObjectId(request.jsonData.id)).map{case (gruntTO: GruntTO, userTO: UserTO) =>
            val htmlResponse = views.html.partials.grunt(gruntTO, Map(userTO.id.get.toString() -> userTO)).body
            Ok(BasicResponse(Some("ok"), Some(htmlResponse)) asJson)
          }.getOrElse(Ok(couldNotFindId asJson))
        }
        case false => {
          Ok(couldNotFindId asJson)
        }
      }
    }.getOrElse(Ok(userNotLoggedIn asJson))
  }
  
  def uploadImage = Action(parse.multipartFormData) { implicit request =>
      request.body.file("picture").map { file =>
        fileManager.storeImage(file.ref.file, file.filename).map { id =>
          Ok(BasicResponse(Some("success"), Some(id.toString)) asJson)
        }.getOrElse(Ok(BasicResponse(Some("failed"), Some("There was an issue storing the file")) asJson))
      }.getOrElse(Ok(BasicResponse(Some("failed"), Some("No file found")) asJson))
  }
  
  def fileUploadMenuPartial = Action { Ok(views.html.partials.fileuploadpopup())}
}
