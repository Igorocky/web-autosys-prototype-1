package services.ssh

class StreamConnector(name: String) {
  private val streamConnectorOut = new StreamConnectorOut(name)

  val input = streamConnectorOut.getInputStream

  val output = streamConnectorOut
}
