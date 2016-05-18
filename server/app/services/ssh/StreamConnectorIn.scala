package services.ssh

import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue

import play.api.Logger

class StreamConnectorIn(name: String) extends InputStream {
  val log: Logger = Logger(this.getClass())
  private val queue = new LinkedBlockingQueue[Int]()

  override def read(): Int = {
    log.trace(s"[$name]reading...")
    val res = queue.take()
    log.trace(s"[$name]read: '${res.toChar}' {$res}")
    res
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val actLen = queue.size() min len
    if (actLen > 0) {
      log.trace(s"[$name]actLen = $actLen")
    }
    Stream.iterate(0)(_ + 1).take(actLen).foreach(i => b(off + i) = read().toByte)
    actLen
  }

  override def available(): Int = queue.size()

  protected[ssh] def write(b: Int): Unit = {
    log.trace(s"[$name]putting '${b.toChar}' {$b}")
    queue.put(b)
  }

  protected[ssh] def write(str: String): Unit = {
    str.view.map(_.toInt).foreach(write)
  }
}
