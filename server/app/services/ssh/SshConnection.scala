package services.ssh

trait SshConnection {
  def exec(command: String, params: Option[Map[String, String]] = None, prompt: Option[String] = None, timeoutMillis: Option[Long] = None): Array[String]

  def validate(): Boolean

  def close(): Unit

}
