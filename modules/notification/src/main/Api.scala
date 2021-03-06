package lila.notification

import scala.collection.mutable

import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }
import play.api.templates.Html

import lila.hub.actorApi.SendTo
import lila.user.User

private[notification] final class Api(socketHub: ActorSelection, renderer: ActorSelection) {

  private val repo = mutable.Map[String, List[Notification]]()
  import makeTimeout.large

  def add(userId: String, html: String, from: Option[String] = None) {
    val notif = Notification(userId, html, from)
    repo.update(userId, notif :: get(userId))
    val request = actorApi.RenderNotification(notif.id, notif.from, notif.html)
    renderer ? request map {
      case rendered: Html ⇒ SendTo(userId, "notificationAdd", rendered.toString)
    } logFailure "[notification] cannot render" pipeToSelection socketHub
  }

  def get(userId: String): List[Notification] = ~(repo get userId)

  def remove(userId: String, id: String) {
    repo.update(userId, get(userId) filter (_.id != id))
    socketHub ! SendTo(userId, "notificationRemove", id)
  }
}
