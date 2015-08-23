package walrath.technology.openwalrus.controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import walrath.technology.openwalrus.daos.UserMongoDao
import walrath.technology.openwalrus.model.tos.User
import walrath.technology.openwalrus.business.UserManager
import javax.inject.Inject

class Application @Inject() (userManager: UserManager) extends Controller {
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def index2 = Action {
    (new UserMongoDao()).create(User(None, "timmay","test@sample.com","samplePass", "Testy", "Testerson", System.currentTimeMillis(), true))
    Ok(views.html.index("Your new application is ready."))
  }

  def read(handle:String, password:String) = Action {
    userManager.login(handle, password) match {
      case Some(x) => Ok("pass")
      case None => Ok("fail")
    }
  }
}
