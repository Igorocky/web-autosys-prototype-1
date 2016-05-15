package services.ssh

import scala.concurrent.Future

trait SshService {
  def executeCommand(connectionTag: SshConnectionTag, command: String, params: Option[Map[String, String]] = None, prompt: Option[String] = None, timeoutMillis: Option[Long] = None): Future[Array[String]]
  def connect(newConnectionTag: SshConnectionTag,
              host: String, port: Int, login: String, password: String): Future[Unit]
}
