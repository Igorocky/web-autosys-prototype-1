package example

import org.scalajs.jquery.JQuery

object JQueryUi {
  implicit def jquery2ui(jquery: JQuery): JQueryUi =
    jquery.asInstanceOf[JQueryUi]
}

trait JQueryUi extends JQuery {
  def dialog(): this.type = ???
}