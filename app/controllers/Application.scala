package controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import walrath.technology.openwalrus.daos.UserMongoDao
import walrath.technology.openwalrus.model.tos.User
import business.UserManager
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import walrath.technology.openwalrus.utils.PhoneAndEmailValidatorUtils._
import java.util.Date
import walrath.technology.openwalrus.model.tos.Grunt
import walrath.technology.openwalrus.model.tos.GruntTO

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
        case (Some(x), _) => Ok(views.html.profile(result._1.get, result._2.getOrElse(List.empty)))
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
  
  def performSignup = Action { implicit request =>
    val (fullName, handle, phoneoremail, password) = signupForm.bindFromRequest.get
    
    if(!userManager.checkIfHandleInUse(handle)) {
      val newUser = User(None, handle, enterEmail(phoneoremail), convertToDomesticPhone(phoneoremail), password, fullName, 0, true, false)
      val result = userManager.createUser(newUser)
      loginRedirect(newUser)
    }
    else {
      Ok("Handle in use")
    }
  }
  
  def loadProfile(handle: String) = Action {
    val result = userManager.getUserProfile(handle)
    result match {
      case (Some(x), _) => Ok(views.html.profile(result._1.get, result._2.getOrElse(List.empty)))
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
  
  private def enterEmail(email: String): Option[String] = if(checkIfPossiblyEmail(email)) Some(email) else None

  private def loginRedirect(user: User): Result = Redirect(routes.Application.index) withSession(
    "userHandle" -> user.handle
  )
}
