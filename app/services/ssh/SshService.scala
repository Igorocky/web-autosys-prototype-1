package services.ssh

import exceptions.AWCException

import scala.concurrent.Future

trait SshService {
  def executeCommand(connectionTag: SshConnectionTag, command: String): Future[String]
  def connect(newConnectionTag: SshConnectionTag,
              host: String, port: Int, login: String, password: String): Future[Either[AWCException, Unit]]
}
