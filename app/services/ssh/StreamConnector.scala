package services.ssh

import java.io.{InputStream, OutputStream}
import java.util.concurrent.LinkedBlockingQueue

class StreamConnector(name: String) extends OutputStream {
  private val inPart = new StreamConnectorIn(name)

  override def write(b: Int): Unit = inPart.write(b)

  def getInputStream = inPart
  def getOutputStream = this

  def write(str: String) = inPart.write(str)
}
