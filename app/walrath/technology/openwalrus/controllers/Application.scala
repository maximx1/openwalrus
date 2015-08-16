package walrath.technology.openwalrus.controllers

import play.api._
import play.api.Play.current
import play.api.mvc._
import walrath.technology.openwalrus.daos.PersonDao
import walrath.technology.openwalrus.model.tos.Person

class Application extends Controller {
  def index = Action {
    (new PersonDao()).storeUser(Person(None, "Timmay", "Turner"))
    Ok(views.html.index("Your new application is ready."))
  }

}
