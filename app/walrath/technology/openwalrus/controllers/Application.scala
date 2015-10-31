package walrath.technology.openwalrus.controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import walrath.technology.openwalrus.daos.UserMongoDao
import walrath.technology.openwalrus.model.tos.User
import walrath.technology.openwalrus.business.UserManager
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import walrath.technology.openwalrus.utils.PhoneAndEmailValidatorUtils._
import java.util.Date

class Application @Inject() (userManager: UserManager) extends Controller {
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def read(handle:String, password:String) = Action {
    userManager.login(handle, password) match {
      case Some(x) => Ok("pass")
      case None => Ok("fail")
    }
  }
  
  def signup = Action {
    Ok(views.html.createuser("English" :: Nil))
  }
  
  val signupForm = Form(
    tuple(
      "fullname" -> nonEmptyText,
      "handle" -> nonEmptyText,
      "phoneoremail" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )
  
  def performSignup = Action { implicit request =>
    val (fullName, handle, phoneoremail, password) = signupForm.bindFromRequest.get
    
    if(!userManager.checkIfHandleInUse(handle)) {
      Ok(userManager.createUser(User(None, handle, enterEmail(phoneoremail), convertToDomesticPhone(phoneoremail), password, fullName, 0, true, false)).toString())
    }
    else {
      Ok("Handle in use")
    }
  }
  
  def loadProfile(handle: String) = Action {
      Ok(handle)
  }
  
  def enterEmail(email: String): Option[String] = if(checkIfPossiblyEmail(email)) Some(email) else None
}
