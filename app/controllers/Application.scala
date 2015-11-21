package controllers
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import walrath.technology.openwalrus.model.tos.{UserTO, User}
import business.UserManager
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import walrath.technology.openwalrus.utils.PhoneAndEmailValidatorUtils._
import walrath.technology.openwalrus.daos.FileGridFsDao
import play.api.libs.iteratee.Enumerator
import com.mongodb.casbah.Imports._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (userManager: UserManager) extends Controller {
  
  val signupForm = Form(
    tuple(
      "fullname" -> nonEmptyText,
      "handle" -> nonEmptyText,
      "phoneoremail" -> nonEmptyText,
      "password" -> nonEmptyText
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
    val (fullName, handle, phoneoremail, password) = signupForm.bindFromRequest.get
    
    if(!userManager.checkIfHandleInUse(handle)) {
      val newUser = User(None, handle, enterEmail(phoneoremail), convertToDomesticPhone(phoneoremail), password, fullName, 0, true, false, None, List.empty, List.empty, List.empty, List.empty)
      val result = userManager.createUser(newUser, request.body.file("picture").map(x => (x.filename, x.ref.file)))
      loginRedirect(newUser)
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
        handleLookUpImage(key)
      }
    }.getOrElse {
      handleLookUpImage(key)
    }
  }

  private def handleLookUpImage(key: String) = key match {
    case "noimage" => Redirect(routes.Assets.versioned("images/noimage.svg"))
    case _ => (new FileGridFsDao).retrieve(new ObjectId(key)).map { fileData =>
      Ok.stream(Enumerator.fromStream(fileData.inputStream)).as(fileData.contentType.getOrElse("image/png")).withHeaders(("ETag" -> key))
    }.getOrElse {
      NotFound
    }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.ApplicationAPI.postGrunt
      )
    ).as("text/javascript")
  }

  private def enterEmail(email: String): Option[String] = if(checkIfPossiblyEmail(email)) Some(email) else None

  private def loginRedirect(user: User): Result = Redirect(routes.Application.index) withSession(
    "userHandle" -> user.handle
  )
}
