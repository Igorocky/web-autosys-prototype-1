package actors

import javax.inject.Inject

import akka.actor.{Actor, Props}
import controllers.IntProp
import dbmappings.Schema
import dbmappings.Schema.intProps
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

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

class PropsHolderActor /*@Inject()(protected val dbConfigProvider: DatabaseConfigProvider)*/ extends Actor /*with HasDatabaseConfigProvider[JdbcProfile]*/ {
  import PropsHolderActor._
  /*import driver.api._*/

  private var props = List[IntProp]()

  override def receive = {
    case GetAll() =>
      /*sender ! Await.result(db.run(intProps.result), 5.seconds).map(Schema.intProp(_)).toList*/
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