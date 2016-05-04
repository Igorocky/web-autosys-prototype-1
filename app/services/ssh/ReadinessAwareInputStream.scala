package services.ssh

import java.io.InputStream

trait ReadinessAwareInputStream extends InputStream {
  def isReady: Boolean
}
