package services.ssh

import org.scalatest.FlatSpec

class StreamConnectorTest extends FlatSpec {
  "StreamConnector" should "return what was written into it" in {
    val streamConnector = new StreamConnector("streamConnector143497")
    val str = "ABCDEFGHI"
    streamConnector.output.write(str)
    val buff = new Array[Byte](100)
    val readCnt = streamConnector.input.read(buff)
    assertResult(str.length)(readCnt)
    assertResult(str)(buff.take(readCnt).map(_.toChar).mkString)
  }

  it should "read nothing if it is empty" in {
    val streamConnector = new StreamConnector("streamConnectorSDFGd")
    val buff = new Array[Byte](100)
    val readCnt = streamConnector.input.read(buff)
    assertResult(0)(readCnt)
  }

  it should "return correct number of available elems" in {
    val streamConnector = new StreamConnector("streamConnectorAS4554")
    val str1 = "ABCDEFGHI"
    streamConnector.output.write(str1)
    val buff = new Array[Byte](100)
    streamConnector.input.read(buff)
    val str2 = "asdfghsdfgh adfg sdfg asd; gh"
    streamConnector.output.write(str2)
    assertResult(str2.length)(streamConnector.input.available())
  }
}
