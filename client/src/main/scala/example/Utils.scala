package example

import example.JQueryUi.jquery2ui
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import shared.SharedMessages

object Utils {
  def openDialog = (e: JQueryEventObject) => jQuery("#" + SharedMessages.DIALOG_DIV_ID).dialog()
}
