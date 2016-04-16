package actors

import akka.actor.{Actor, Props}
import controllers.IntProp

object PropsHolderActor {
  def props = Props[PropsHolderActor]

  case class Add(prop: IntProp)
  case class GetAll()

  case class Success()
  case class Error(msg: String, props: List[IntProp])
}

class PropsHolderActor extends Actor {
  import PropsHolderActor._

  private var props = List[IntProp]()

  override def receive = {
    case GetAll() =>
      sender ! props
    case Add(prop) =>
      if (props.exists(_.key == prop.key)) {
        sender ! Error(s"Property with key='${prop.key}' already exists", props)
      } else {
        props ::= prop
        sender ! Success()
      }
  }
}