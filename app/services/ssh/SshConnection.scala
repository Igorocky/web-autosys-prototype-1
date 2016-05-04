package services.ssh

trait SshConnection {
  def exec(command: String): String

  def validate(): Boolean

  def close(): Unit

}
