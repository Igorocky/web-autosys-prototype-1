package services.ssh

import java.io.{BufferedWriter, OutputStreamWriter, _}

import exceptions.AWCException
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannel
import org.apache.sshd.client.future.ConnectFuture
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.Closeable
import org.apache.sshd.common.channel.Channel
import org.apache.sshd.common.util.io.{NoCloseInputStream, NoCloseOutputStream}
import play.api.Logger

import scala.collection.immutable.WrappedString

class SshConnection1(host: String, port: Int, login: String, password: String) extends SshConnection {
  val log: Logger = Logger(this.getClass())

  private final val PROMPT: String = "#:#:#:>>>"
  private final val PROMPT_SEQ: WrappedString = wrapString(PROMPT)

  private val (
    client: SshClient,
    session: ClientSession,
    channel: ClientChannel,
    output: StreamConnector,
    input: ReadinessAwareInputStream
    ) = connect(host, port, login, password)

  exec("PS1=\"" + PROMPT + "\"")
//  exec("date")

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
    try {
      close(channel)
    } finally {
      try {
        close(session)
      } finally {
        close(client)
      }
    }
  }

  private def writeCmd(cmd: String): Unit = {
    output.write(cmd + "\n\n")
  }

  private def isReady(): Boolean = {
    input.isReady
  }

  private def read(): Char = {
    input.read().toChar
  }

  private def checkTimeout(startMillis: Long, timeout: Long): Unit = {
    if (System.currentTimeMillis() - startMillis > timeout) {
      throw new AWCException("timeout in readTillPrompt")
    }
  }

  private def readTillPrompt(prompt: String = PROMPT): String = {
    val promptSeq: WrappedString = wrapString(prompt)
    val startMillis: Long = System.currentTimeMillis()
    val TIMEOUT: Long = 30000L
    val SLEEP_MILLIS = 50L

    var result: String = null
    val buff = new StringBuilder()
    while (result == null) {
      checkTimeout(startMillis, TIMEOUT)
      if (!isReady()) {
        Thread.sleep(SLEEP_MILLIS)
      } else {
        while (isReady()) {
          checkTimeout(startMillis, TIMEOUT)
          buff.append(read())
        }

        val str = buff.toString()
        log.debug(s"buf ends with '${str.substring((str.length - 10) max 0)}'")
        log.debug(s"expecting '${promptSeq}'")
        if (str.endsWith(prompt)) {
          result = buff.toString()
        }
      }
    }
    log.debug(s"SUCCESS!!! result = $result")
    result
  }

  private def connect(host: String, port: Int, login: String, password: String): (SshClient, ClientSession, ClientChannel, StreamConnector, ReadinessAwareInputStream) = {
    try {
      val client: SshClient = SshClient.setUpDefaultClient()
      client.start()

      val connectFuture: ConnectFuture = client.connect(login, host, port)
      connectFuture.await()
      val session: ClientSession = connectFuture.getSession()

      session.addPasswordIdentity(password)
      session.auth().verify(1000)
      val channel = session.createChannel(Channel.CHANNEL_SHELL)
      val userOutputStream = new StreamConnector("userOutputStream")
      val userInputStream = new StreamConnector("userInputStream")
      channel.setIn(new NoCloseInputStream(userOutputStream.getInputStream))
      channel.setOut(new NoCloseOutputStream(userInputStream.getOutputStream))
//      channel.setOut(new NoCloseOutputStream(System.out))
      channel.setErr(new NoCloseOutputStream(userInputStream.getOutputStream))
//      channel.setErr(new NoCloseOutputStream(System.out))
      channel.open()


      Thread.sleep(1000)

      (client, session, channel, userOutputStream, userInputStream.getInputStream)
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
