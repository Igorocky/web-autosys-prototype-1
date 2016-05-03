package services.ssh

import java.io.{BufferedWriter, InputStreamReader, OutputStreamWriter, _}

import exceptions.AWCException
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannel
import org.apache.sshd.client.future.ConnectFuture
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.Closeable
import org.apache.sshd.common.channel.Channel
import org.apache.sshd.common.util.io.{NoCloseInputStream, NoCloseOutputStream}

import scala.collection.immutable.WrappedString

class SshConnection(host: String, port: Int, login: String, password: String) {
  private final val PROMPT: String = "#:#:#:>>>"
  private final val PROMPT_SEQ: WrappedString = wrapString(PROMPT)

  private val (
    client: SshClient,
    session: ClientSession,
    channel: ClientChannel,
    outputStream: OutputStream,
    inputStream: InputStream,
    errInputStream: InputStream
    ) = connect(host, port, login, password)

  private val input: BufferedReader = new BufferedReader(new InputStreamReader(inputStream))
  private val errorInput: BufferedReader = new BufferedReader(new InputStreamReader(errInputStream))
  private val output: BufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream))

  exec("PS1=\"" + PROMPT + "\"")

  def exec(command: String): String = {
    try {
      writeCmd(command)
      readTillPrompt()
    } catch {
      case ex: Exception =>
        close()
        throw ex
    }
  }

  def validate(): Boolean = {
    true
  }

  def close(): Unit = {
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
    output.write(cmd)
    output.newLine()
    output.flush()
  }

  private def isReady(): Boolean = {
    errorInput.ready() || input.ready()
  }

  private def read(): Char = {
    if (errorInput.ready()) errorInput.read().toChar else input.read().toChar
  }

  private def checkTimeout(startMillis: Long, timeout: Long): Unit = {
    if (System.currentTimeMillis() - startMillis > timeout) {
      throw new AWCException("timeout in readTillPrompt")
    }
  }

  private def readTillPrompt(): String = {
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
        if (buff.endsWith(PROMPT_SEQ)) {
          result = buff.toString()
        }
      }
    }
    result
  }

  private def connect(host: String, port: Int, login: String, password: String): (SshClient, ClientSession, ClientChannel, PipedOutputStream, PipedInputStream, PipedInputStream) = {
    try {
      val client: SshClient = SshClient.setUpDefaultClient()
      client.start()

      val connectFuture: ConnectFuture = client.connect(login, host, port)
      connectFuture.await()
      val session: ClientSession = connectFuture.getSession()

      session.addPasswordIdentity(password)
      session.auth().verify(1000)
      val channel = session.createChannel(Channel.CHANNEL_SHELL)
      val userOutputStream = new PipedOutputStream()
      val userInputStream = new PipedInputStream()
      val userErrorInputStream = new PipedInputStream()

      channel.setIn(new NoCloseInputStream(new PipedInputStream(userOutputStream)))
      channel.setOut(new NoCloseOutputStream(new PipedOutputStream(userInputStream)))
      channel.setErr(new NoCloseOutputStream(new PipedOutputStream(userErrorInputStream)))
      channel.open()

      (client, session, channel, userOutputStream, userInputStream, userErrorInputStream)
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
