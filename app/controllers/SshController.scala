package controllers

import javax.inject._

import play.api.mvc._
import services.ssh.{SshConnectionTag, SshService}

import scala.concurrent.ExecutionContext

@Singleton
class SshController @Inject()(sshService: SshService)(implicit exec: ExecutionContext) extends Controller {

  def date = Action.async {
    sshService.executeCommand(SshConnectionTag("localhost-tag"), "date").map(r => Ok(views.html.ssh.date(r)))
  }
}
