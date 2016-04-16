package actors

import akka.actor.{Actor, Props}
import controllers.IntProp

object PropsHolderActor {
  def props = Props[PropsHolderActor]

  case class GetAll()
  case class Add(prop: IntProp)
  case class Remove(key: String)
  case class Get(key: String)
  case class Change(prop: IntProp)

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
    case Change(prop) =>
      if (!props.exists(_.key == prop.key)) {
        sender ! Error(s"Property with key='${prop.key}' doesn't exists", props)
      } else {
        props = prop :: props.filterNot(_.key == prop.key)
        sender ! Success()
      }
    case Remove(key) =>
      if (!props.exists(_.key == key)) {
        sender ! Error(s"Property with key='${key}' doesn't exists", props)
      } else {
        props = props.filterNot(_.key == key)
        sender ! Success()
      }
    case Get(key) =>
      if (!props.exists(_.key == key)) {
        sender ! Error(s"Property with key='${key}' doesn't exists", props)
      } else {
        sender ! props.find(_.key == key).get
      }
  }
}