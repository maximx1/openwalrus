package controllers

import javax.inject.{Inject, Singleton}

import play.api.Environment
import play.api.mvc.*

/**
 * Serves the web client that Vite builds into public/. Any path that isn't the API or a
 * file of the client gets index.html, and the client's router takes it from there.
 */
@Singleton
class FrontendController @Inject() (cc: ControllerComponents, assets: Assets, env: Environment) extends AbstractController(cc) {

  def index: Action[AnyContent] = route("")

  def route(path: String): Action[AnyContent] =
    if (path.nonEmpty && !path.contains("/") && env.resource(s"public/$path").isDefined) assets.at("/public", path)
    else if (env.resource("public/index.html").isDefined) assets.at("/public", "index.html")
    else Action(NotFound("The web client has not been built. Run `npm run build` in web/, or use the Vite dev server (see README.md)."))
}
