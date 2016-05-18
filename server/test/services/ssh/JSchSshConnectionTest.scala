package services.ssh

import org.scalatest.FlatSpec

class JSchSshConnectionTest extends FlatSpec {
  "JSchSshConnection.substituteParams" should "substitute all params" in {
    assertResult("echo QWERT1QWERT2 QWERT3"){
      JSchSshConnection.substituteParams(
        "echo ${pass1}${pass2} ${pass3}",
        Map(
          "pass1" -> "QWERT1",
          "pass2" -> "QWERT2",
          "pass3" -> "QWERT3"
        )
      )
    }
  }

  it should "leave not defined params as is" in {
    assertResult("echo ${pass1}${pass2} ${pass3}"){
      JSchSshConnection.substituteParams("echo ${pass1}${pass2} ${pass3}", Map())
    }
  }

  it should "not fail on empty string" in {
    assertResult(""){
      JSchSshConnection.substituteParams("", Map())
    }
  }

  it should "not fail on a string without params" in {
    assertResult("echo date"){
      JSchSshConnection.substituteParams(
        "echo date",
        Map(
          "pass1" -> "QWERT1",
          "pass2" -> "QWERT2",
          "pass3" -> "QWERT3"
        )
      )
    }
  }
}
