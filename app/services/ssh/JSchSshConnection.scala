package services.ssh

import com.jcraft.jsch._
import exceptions.AWCException
import play.api.Logger
import services.ssh.JSchSshConnection.substituteParams

class JSchSshConnection(host: String, port: Int, login: String, password: String) extends SshConnection {
  val log: Logger = Logger(this.getClass())

  private val connectionName = s"$login@$host:$port"

  private val DEFAULT_PROMPT: String = "#:#:#:>>>"
  private val TIMEOUT_MILLIS: Long = 30000

  private val (
    session: Session,
    channel: Channel,
    output: StreamConnectorOut,
    input: StreamConnectorIn
    ) = connect(host, port, login, password)

  exec("PS1=\"" + DEFAULT_PROMPT + "\"")

  override def exec(command: String, params: Option[Map[String, String]] = None, prompt: Option[String] = None, timeoutMillis: Option[Long] = None): Array[String] = {
    try {
      log.debug(s"$connectionName: executing command '$command'")
      writeCmd(substituteParams(command, params.getOrElse(Map())))
      val res = readTillPrompt(prompt.getOrElse(DEFAULT_PROMPT), timeoutMillis.getOrElse(TIMEOUT_MILLIS))
      log.debug(s"$connectionName: result = '$res'")
      res.split("\n")
    } catch {
      case ex: Exception =>
        log.error(s"Exception while executing command '$command': ${ex.getMessage}", ex)
        close()
        throw ex
    }
  }

  override def validate(): Boolean = {
    true
  }

  override def close(): Unit = {
    if (channel != null) {
      try {
        channel.disconnect()
      } catch {
        case ex: Exception =>
          log.error(s"Exception while closing channel: ${ex.getMessage}", ex)
      }
    }
    if (session != null) {
      try {
        session.disconnect()
      } catch {
        case ex: Exception =>
          log.error(s"Exception while closing session: ${ex.getMessage}", ex)
      }
    }
  }

  private def writeCmd(cmd: String): Unit = {
    output.write(cmd + "\r\n")
  }

  private def readStrOrWait(waitMillis: Long = 50L): String = {
    log.trace(s"$connectionName: readStrOrWait start")
    if (input.available() > 0) {
      log.trace(s"$connectionName: readStrOrWait available = ${input.available()}")
      val buf = new Array[Byte](1000)
      val readCnt = input.read(buf)
      val res = buf.view.take(readCnt).map(_.toChar).mkString
      log.trace(s"$connectionName: readStrOrWait: res = '$res'")
      res
    } else {
      log.trace(s"$connectionName: readStrOrWait: no input, waiting $waitMillis")
      Thread.sleep(waitMillis)
      ""
    }
  }

  private def checkTimeout(startMillis: Long, timeoutMillis: Long): Unit = {
    if (System.currentTimeMillis() - startMillis > timeoutMillis) {
      throw new AWCException(s"$connectionName: timeout in readTillPrompt")
    }
  }

  private def readTillPrompt(prompt: String, timeout: Long): String = {
    log.trace(s"$connectionName: enter readTillPrompt")
    val startMillis: Long = System.currentTimeMillis()
    val SLEEP_MILLIS = 50L
    val promptTrimmed = prompt.trim

    var result: String = null
    val buff = new StringBuilder()
    while (result == null) {
      checkTimeout(startMillis, timeout)
      log.trace(s"$connectionName: %%%1")
      var str: String = readStrOrWait(SLEEP_MILLIS)
      while (str != "") {
        log.trace(s"$connectionName: %%%2")
        checkTimeout(startMillis, timeout)
        buff.append(str)
        str = readStrOrWait(SLEEP_MILLIS)
      }
      val bufStr = buff.toString()
      buff.clear()
      buff.append(bufStr)
      log.trace(s"$connectionName: buf ends with '${bufStr.substring(bufStr.length - ((prompt.length + 10) min bufStr.length))}'")
      log.trace(s"$connectionName: expecting '${promptTrimmed}'")
      if (bufStr.trim.endsWith(promptTrimmed)) {
        log.trace(s"$connectionName: %%%3")
        result = bufStr
      }
    }
    log.trace(s"$connectionName: SUCCESS!!! result = '$result'")
    log.trace(s"$connectionName: exit readTillPrompt")
    result
  }

  private def connect(host: String, port: Int, login: String, password: String): (Session, Channel, StreamConnectorOut, StreamConnectorIn) = {
    try {
      log.debug(s"$connectionName: connecting...")
      val jsch = new JSch
      val session = jsch.getSession(login, host, port)
      session.setPassword(password)
      val ui = new MyUserInfo
      session.setUserInfo(ui)
      session.connect(30000)
      log.debug(s"$connectionName: session connected")
      val channel=session.openChannel("shell")
      log.debug(s"$connectionName: shell channel opened")

      val userOutputStream = new StreamConnector(s"$connectionName.userOutputStream")
      val userInputStream = new StreamConnector(s"$connectionName.userInputStream")
      channel.setInputStream(userOutputStream.input)
      channel.setOutputStream(userInputStream.output)

      channel.connect()
      log.debug(s"$connectionName: channel connected")

      (session, channel, userOutputStream.output, userOutputStream.input)
    } catch {
      case ex: Exception =>
        log.error(s"Exception while connecting to $connectionName: " + ex.getMessage)
        close()
        throw ex
    }
  }
}

class MyUserInfo extends UserInfo with UIKeyboardInteractive {
  override def getPassword() = null
  override def promptYesNo(str: String) = true
  override def getPassphrase() = null
  override def promptPassphrase(message: String) = false
  override def promptPassword(message: String) = false
  override def showMessage(message: String) {}
  override def promptKeyboardInteractive(destination: String,
                                         name: String,
                                         instruction: String,
                                         prompt: Array[String],
                                         echo: Array[Boolean]) = null
}

object JSchSshConnection {
  private val paramPattern = """\$\{([^\}]+)\}""".r
  def substituteParams(pattern: String, params: Map[String,String]): String = {
    def getValue(key: String) = if (params.contains(key)) params(key) else "${" + key + "}"

    paramPattern.findAllMatchIn(pattern).foldLeft((pattern, List[String]())){
      case ((pattern, replacedPlaceholders), matc) =>
        if (replacedPlaceholders.contains(matc.group(1))) {
          (pattern, replacedPlaceholders)
        } else {
          (pattern.replaceAllLiterally("${" + matc.group(1) + "}", getValue(matc.group(1))), matc.group(1)::replacedPlaceholders)
        }
    }._1
  }
}