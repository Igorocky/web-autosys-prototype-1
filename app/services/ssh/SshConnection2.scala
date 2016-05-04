package services.ssh

import java.io.{BufferedWriter, InputStreamReader, OutputStreamWriter, _}

import com.jcraft.jsch._
import exceptions.AWCException
import play.api.Logger

import scala.collection.immutable.WrappedString

class SshConnection2(host: String, port: Int, login: String, password: String) extends SshConnection {
  val log: Logger = Logger(this.getClass())

  private final val PROMPT: String = "#:#:#:>>>"
  private final val PROMPT_SEQ: WrappedString = wrapString(PROMPT)

  private val (
    session: Session,
    channel: Channel,
    outputStream: OutputStream,
    inputStream: InputStream
    ) = connect(host, port, login, password)

//  private val output: BufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream))
//  private val input: BufferedReader = new BufferedReader(new InputStreamReader(inputStream))

  exec("PS1=\"" + PROMPT + "\"")

  override def exec(command: String): String = {
    try {
      writeCmd(command)
      readTillPrompt()
    } catch {
      case ex: Exception =>
        close()
        throw ex
    }
  }

  override def validate(): Boolean = {
    true
  }

  override def close(): Unit = {
    channel.disconnect()
    session.disconnect()
  }

  private def writeCmd(cmd: String): Unit = {
    log.debug("enter writeCmd")
//    output.write(cmd)
//    output.newLine()
//    output.flush()
    outputStream.write((cmd + "\r\n").toCharArray.map(_.toByte))
    outputStream.flush()
    log.debug("exit writeCmd")
  }

//  private def isReady(): Boolean = {
//    input.ready()
//  }

//  private def read(): Char = {
//    input.read().toChar
//  }

  private def readStr(): String = {
    log.debug(s"readStr start")
    if (inputStream.available() > 0) {
      log.debug(s"readStr available = ${inputStream.available()}")
      val arr = new Array[Byte](1000)
      val readCnt = inputStream.read(arr)
      val res = arr.take(readCnt).map(_.toChar).mkString("")
      log.debug(s"readStr: res = '$res'")
      res
    } else {
      Thread.sleep(50)
      ""
    }

  }

  private def checkTimeout(startMillis: Long, timeout: Long): Unit = {
    if (System.currentTimeMillis() - startMillis > timeout) {
      throw new AWCException("timeout in readTillPrompt")
    }
  }

  private def readTillPrompt(): String = {
    log.debug("enter readTillPrompt")
    val startMillis: Long = System.currentTimeMillis()
    val TIMEOUT: Long = 30000L
    val SLEEP_MILLIS = 50L

    var result: String = null
    val buff = new StringBuilder()
    while (result == null) {
      checkTimeout(startMillis, TIMEOUT)
      log.debug("%%%1")
//      if (!isReady()) {
//        log.debug("%%%2")
//        Thread.sleep(SLEEP_MILLIS)
//      } else {
        log.debug("%%%3")
      var str2: String = readStr()
        while (/*isReady()*/ str2 != "") {
          log.debug("%%%4")
          checkTimeout(startMillis, TIMEOUT)
          buff.append(/*read()*/str2)
          str2 = readStr()
        }
        val str = buff.toString()
        log.debug(s"buf ends with '${str.substring(str.length - (10 min str.length))}'")
        log.debug(s"expecting '${PROMPT_SEQ}'")
        if (buff.endsWith(PROMPT_SEQ)) {
          log.debug("%%%5")
          result = buff.toString()
        }
//      }
    }
    log.debug(s"SUCCESS!!! result = $result")
    log.debug("exit readTillPrompt")
    result
  }

  private def connect(host: String, port: Int, login: String, password: String): (Session, Channel, OutputStream, InputStream) = {
    try {
      val jsch = new JSch
      val session = jsch.getSession(login, host, port)
      session.setPassword(password)
      val ui = new MyUserInfo(){}
      session.setUserInfo(ui)
      session.connect(30000)
      val channel=session.openChannel("shell")

//      val userOutputStream = new StreamConnector("userOutputStream")
//      val userInputStream = new StreamConnector("userInputStream")
//      channel.setInputStream(userOutputStream.getInputStream)
//      channel.setOutputStream(userInputStream.getOutputStream)

      channel.connect()
//      val dataIn = new DataInputStream(channel.getInputStream());
//      val dataOut = new DataOutputStream(channel.getOutputStream());

//      (session, channel, dataOut, dataIn)
      (session, channel, channel.getOutputStream(), channel.getInputStream())
    } catch {
      case ex: Exception =>
        close()
        throw ex
    }
  }

  private def close(closable: Closeable): Unit = {
    if (closable != null) {
      closable.close()
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