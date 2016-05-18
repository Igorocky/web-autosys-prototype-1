package example

import org.scalajs.jquery.jQuery
import shared.SharedMessages

import scala.scalajs.js

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {
//    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
//    jQuery("#dialog").hide()
    jQuery("#" + SharedMessages.CLICK_ME_BUTTON_ID).click(Utils.openDialog)
  }
}