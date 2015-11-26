package controllers
import play.api.Play.current
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import models.{UserTO, User}
import business.UserManager
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import core.utils.PhoneAndEmailValidatorUtils._
import data.daos.FileGridFsDao
import play.api.libs.iteratee.Enumerator
import com.mongodb.casbah.Imports._
import scala.concurrent.ExecutionContext.Implicits.global
import business.FileManager

class Application @Inject() (userManager: UserManager, fileManager: FileManager) extends Controller {
  
  val signupForm = Form(
    tuple(
      "fullname" -> nonEmptyText,
      "handle" -> nonEmptyText,
      "phoneoremail" -> nonEmptyText,
      "password" -> nonEmptyText,
      "profileImage" -> text
    )
  )
  
  val signinForm = Form(
    tuple(
      "handle" -> nonEmptyText,
      "password" -> nonEmptyText
    )    
  )
  
  def index = Action { implicit request =>
    request.session.get("userHandle").map { handle =>
      val result = userManager.getUserProfile(handle)
      result match {
        case (Some(x), _, _) => Ok(views.html.profile(UserTO.fromUser(result._1.get), result._2.getOrElse(List.empty), result._3)(request.session))
        case _ => Redirect(routes.Application.loadLogin)
      }
    }.getOrElse {
      Ok(views.html.index())
    }
  }

  def read(handle:String, password:String) = Action {
    userManager.login(handle, password) match {
      case Some(x) => Ok("pass")
      case None => Ok("fail")
    }
  }
  
  def signup = Action { implicit request =>
    request.session.get("userHandle").map { handle =>
      Redirect(routes.Application.index)
    }.getOrElse {
      Ok(views.html.createuser("English" :: Nil))
    }
  }
  
  def performSignup = Action(parse.multipartFormData) { implicit request =>
    val (fullName, handle, phoneoremail, password, profileImage) = signupForm.bindFromRequest.get
    val profileImageId = org.bson.types.ObjectId.isValid(profileImage) match {
      case true => Some(new ObjectId(profileImage))
      case false => None
    }
    
    if(!userManager.checkIfHandleInUse(handle)) {
      val newUser = User(None, handle, enterEmail(phoneoremail), convertToDomesticPhone(phoneoremail), password, fullName, 0, true, false, profileImageId, List.empty, List.empty, List.empty, List.empty)
      val result = userManager.createUser(newUser)
      result.map(newId => loginRedirect(newUser.copy(id = Some(newId)))).getOrElse(Ok("There was an issue creating the user"))
    }
    else {
      Ok("Handle in use")
    }
  }
  
  def loadProfile(handle: String) = Action { implicit request =>
    val result = userManager.getUserProfile(handle)
    result match {
      case (Some(x), _, _) => {result._3.get("asdf").map(_.profileImage).getOrElse("noimage"); Ok(views.html.profile(UserTO.fromUser(result._1.get), result._2.getOrElse(List.empty), result._3)(request.session))}
      case _ => Ok("Profile not found")
    }
  }

  def loadLogin = Action { implicit request =>
    request.session.get("userHandle").map { handle =>
      Redirect(routes.Application.index)
    }.getOrElse {
      Ok(views.html.login(None))
    }
  }

  def attemptSignin = Action { implicit request =>
    val (handle, password) = signinForm.bindFromRequest.get
    userManager.login(handle, password).map { user => 
      loginRedirect(user)
    }.getOrElse {
      Ok(views.html.login(Some("Username and password did not match...")))
    }
  }
  
  def logout = Action {
    Redirect(routes.Application.index()) withNewSession
  }
  
  def followingPage(handle: String) = TODO
  
  def followersPage(handle: String) = TODO
  
  def lookUpImage(key: String) = Action { implicit request =>
    request.headers.get("If-None-Match").map { ifNoneMatch =>
      if(ifNoneMatch == key) {
        NotModified
      }
      else {
        handleLookUpImage(key, false)
      }
    }.getOrElse {
      handleLookUpImage(key, false)
    }
  }
  
  def lookUpImageThumb(key: String) = Action { implicit request =>
    request.headers.get("If-None-Match").map { ifNoneMatch =>
      if(ifNoneMatch == key) {
        NotModified
      }
      else {
        handleLookUpImage(key, true)
      }
    }.getOrElse {
      handleLookUpImage(key, true)
    }
  }

  private def handleLookUpImage(key: String, isThumb: Boolean) = key match {
    case "noimage" => Redirect(routes.Assets.versioned("images/noimage.svg"))
    case _ => {
      org.bson.types.ObjectId.isValid(key) match {
        case true => {
          fileManager.lookUpImage(new ObjectId(key), isThumb).map { fileData =>
            Ok.stream(Enumerator.fromStream(fileData.inputStream)).as(fileData.contentType.getOrElse("image/png")).withHeaders(("ETag" -> key))
          }.getOrElse {
            NotFound
          }
        }
        case false => {
          NotFound
        }
      }
    }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.ApplicationAPI.postGrunt,
        routes.javascript.ApplicationAPI.retrieveSingleGrunt,
        routes.javascript.ApplicationAPI.fileUploadMenuPartial
      )
    ).as("text/javascript")
  }

  private def enterEmail(email: String): Option[String] = if(checkIfPossiblyEmail(email)) Some(email) else None

  private def loginRedirect(user: User): Result = Redirect(routes.Application.index) withSession(
    "userHandle" -> user.handle,
    "userId" -> user.id.get.toString
  )
}
