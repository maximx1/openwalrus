package walrath.technology.openwalrus.controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import walrath.technology.openwalrus.daos.UserDao
import walrath.technology.openwalrus.model.tos.User

class Application extends Controller {
  def index = Action {
    //(new PersonDao()).storeUser(User(None, "Timmay", "Turner"))
    Ok(views.html.index("Your new application is ready."))
  }

}
