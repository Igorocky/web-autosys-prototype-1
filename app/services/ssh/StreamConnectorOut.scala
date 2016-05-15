package services.ssh

import java.io.OutputStream

class StreamConnectorOut(name: String) extends OutputStream {
  private val inPart = new StreamConnectorIn(name)

  def getInputStream = inPart

  def getOutputStream = this

  override def write(b: Int): Unit = inPart.write(b)

  def write(str: String) = inPart.write(str)
}
