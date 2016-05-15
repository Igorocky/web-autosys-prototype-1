package services.ssh

import org.scalatest.FlatSpec

class StreamConnectorTest extends FlatSpec {
  "StreamConnector" should "return what was written into it" in {
    val streamConnector = new StreamConnector("streamConnector143497")
    val str = "ABCDEFGHI"
    streamConnector.write(str)
    val buff = Array.fill[Byte](100)(0)
    val readCnt = streamConnector.getInputStream.read(buff)
    assertResult(str.length)(readCnt)
    assertResult(str)(buff.take(readCnt).map(_.toChar).mkString)
  }

  it should "read nothing if it is empty" in {
    val streamConnector = new StreamConnector("streamConnector143497")
    val buff = Array.fill[Byte](100)(0)
    val readCnt = streamConnector.getInputStream.read(buff)
    assertResult(0)(readCnt)
  }

  it should "return correct number of available elems" in {
    val streamConnector = new StreamConnector("streamConnector143497")
    val str1 = "ABCDEFGHI"
    streamConnector.write(str1)
    val buff = Array.fill[Byte](100)(0)
    streamConnector.getInputStream.read(buff)
    val str2 = "asdfghsdfgh adfg sdfg asd; gh"
    streamConnector.write(str2)
    assertResult(str2.length)(streamConnector.getInputStream.available())
  }
}
