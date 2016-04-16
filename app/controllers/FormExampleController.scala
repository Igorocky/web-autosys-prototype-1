package controllers

import javax.inject.{Inject, Named, Singleton}

import actors.PropsHolderActor.{Add, Error, GetAll, Success}
import akka.actor.ActorRef
import akka.pattern.ask
import play.api.data.{FormError, Form}
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Valid, ValidationError, Invalid, Constraint}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.format.Formats._

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

  implicit val timeout = Timeout(5.seconds)

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
        onErrors = (formWithErrors: Form[IntProp]) => BadRequest(views.html.props(Nil, formWithErrors))
      )
  ){request =>
    (propsHolderActor ? Add(request.body)).map{
      case Success() => Redirect(routes.FormExampleController.allProps)
      case e: Error => BadRequest(views.html.props(Nil, propForm.fill(request.body)))
    }
  }
}
