package lila.app

import play.api.mvc._
import play.api.mvc.Results._
import play.api.{ Application, GlobalSettings, Mode }

import lila.hub.actorApi.monitor.AddRequest

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lila.app.Env.current
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] =
    if (Env.ai.isServer) {
      if (req.path startsWith "/ai/") super.onRouteRequest(req)
      else Action(NotFound("I am an AI server")).some
    }
    else {
      Env.monitor.reporting ! AddRequest
      // Env.security.wiretap(req)
      Env.security.firewall.requestHandler(req).await orElse
        Env.i18n.requestHandler(req) orElse
        super.onRouteRequest(req)
    }

  override def onHandlerNotFound(req: RequestHeader) =
    Env.ai.isServer.fold[Fu[SimpleResult]](
      fuccess(NotFound),
      controllers.Lobby.handleNotFound(req))

  override def onBadRequest(req: RequestHeader, error: String) = fuccess {
    BadRequest("Bad Request: " + error)
  }

  override def onError(request: RequestHeader, ex: Throwable) =
    Env.ai.isServer.fold(
      fuccess(InternalServerError(ex.getMessage)),
      lila.common.PlayApp.isProd.fold(
        fuccess(InternalServerError(views.html.base.errorPage(ex)(lila.user.Context(request, none)))),
        super.onError(request, ex)
      )
    )

}
