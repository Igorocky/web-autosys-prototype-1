package controllers

import javax.inject.{Inject, Named, Singleton}

import actors.PropsHolderActor.{Add, Error, GetAll, Success}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}

import scala.concurrent.Await
import scala.concurrent.duration._

case class IntProp(key: String, value: Int) {

}

object IntProp {
  val KEY = "key"
  val VALUE = "value"
}

@Singleton
class FormExampleController @Inject()(
                                       @Named("props-holder-actor") propsHolderActor: ActorRef,
                                       val messagesApi: MessagesApi
                                     ) extends Controller with I18nSupport {

  implicit val akkaAskTimeout = Timeout(5.seconds)

  val propForm = Form(
    mapping(
      IntProp.KEY -> nonEmptyText,
      IntProp.VALUE -> number(min = 0, max = 100)
    )(IntProp.apply)(IntProp.unapply)
  )

  def allProps = Action.async(
    (propsHolderActor ? GetAll()).map{
      case list: List[IntProp] @unchecked => Ok(views.html.props(list, propForm))
    }
  )

  def addProp = Action.async(
      parse.form(
        propForm,
        onErrors = (formWithErrors: Form[IntProp]) => BadRequest(views.html.props(getAllProps, formWithErrors))
      )
  ){request =>
    (propsHolderActor ? Add(request.body)).map{
      case Success() => Redirect(routes.FormExampleController.allProps)
      case e: Error => BadRequest(views.html.props(e.props, propForm.fill(request.body), Some(e.msg)))
    }
  }

  private def getAllProps = Await.result((propsHolderActor ? GetAll()).map(_.asInstanceOf[List[IntProp]]), 5.seconds)
}
