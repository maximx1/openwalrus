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
import scala.util.Random

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
  
  val welcomeMessage = "Welcome to the winning team"
  val pleaseJoinMessage = "Come join the winning team"
  
  def index = Action { implicit request =>
    request.session.get("userHandle").map { handle =>
      userManager.getUserProfile(handle) match {
        case (Some(user), grunts, gruntProfilesData) => Ok(views.html.profile(user, grunts, gruntProfilesData)(request.session))
        case _ => Redirect(routes.Application.loadLogin)
      }
    }.getOrElse {
      Ok(views.html.cardview(pleaseJoinMessage, Random.shuffle(userManager.getUserProfiles))(request.session))
    }
  }
  
  def team = Action { implicit request =>
    Ok(views.html.cardview(
        request.session.get("userHandle").map(x => welcomeMessage).getOrElse(pleaseJoinMessage),
        Random.shuffle(userManager.getUserProfiles)
    )(request.session))
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
      val newUser = User(None, handle, enterEmail(phoneoremail), convertToDomesticPhone(phoneoremail), password, fullName, 0, true, false, profileImageId, None, List.empty, List.empty, List.empty, List.empty)
      val result = userManager.createUser(newUser)
      result.map(newId => loginRedirect(newUser.copy(id = Some(newId)))).getOrElse(Ok("There was an issue creating the user"))
    }
    else {
      Ok("Handle in use")
    }
  }
  
  def loadProfile(handle: String) = Action { implicit request =>
    userManager.getUserProfile(handle) match {
      case (Some(user), grunts, gruntProfilesData) => Ok(views.html.profile(user, grunts, gruntProfilesData)(request.session))
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
  
  def followingPage(handle: String) = Action { implicit request =>
    Ok(views.html.cardview("Following", userManager.getFollowing(handle))(request.session))
  }
  
  def followersPage(handle: String) = Action { implicit request =>
    Ok(views.html.cardview("Followers", userManager.getFollowers(handle))(request.session))
  }
  
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
        routes.javascript.ApplicationAPI.fileUploadMenuPartial,
        routes.javascript.Application.lookUpImageThumb,
        routes.javascript.Application.lookUpImage,
        routes.javascript.ApplicationAPI.updateProfileImage,
        routes.javascript.ApplicationAPI.updateBannerImage,
        routes.javascript.ApplicationAPI.updateFollowingStatus
      )
    ).as("text/javascript")
  }

  private def enterEmail(email: String): Option[String] = if(checkIfPossiblyEmail(email)) Some(email) else None

  private def loginRedirect(user: User): Result = Redirect(routes.Application.index) withSession(
    "userHandle" -> user.handle,
    "userId" -> user.id.get.toString
  )
}
