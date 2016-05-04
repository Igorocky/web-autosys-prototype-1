package services.ssh

import java.util.concurrent.LinkedBlockingQueue

import play.api.Logger

class StreamConnectorIn(name: String) extends ReadinessAwareInputStream {
  val log: Logger = Logger(this.getClass())
  private val queue = new LinkedBlockingQueue[Int]()

  override def read(): Int = {
    log.debug(s"[$name]reading...")
    val res = queue.take()
//    val res = if (isReady) queue.take() else -1
    log.debug(s"[$name]read: '${res.toChar}' {$res}")
    res
  }


  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val actLen = queue.size() min len
    if (actLen > 0) {
      log.debug(s"[$name]actLen = $actLen")
    }
    (1 to actLen).foreach(i => b(off + i) = read().toByte)
    actLen
  }


  override def available(): Int = queue.size()

  def write(b: Int): Unit = {
    log.debug(s"[$name]puting '${b.toChar}' {$b}")
    queue.put(b)
  }

  def write(str: String): Unit = {
    str.map(_.toInt).foreach(write)
  }

  override def isReady: Boolean = !queue.isEmpty
}
