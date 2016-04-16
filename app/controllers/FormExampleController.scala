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

  val valueConstraint: Constraint[Int] = Constraint("valueConstraint"){value=>
    if (value < 0 || value > 100) {
      Invalid(ValidationError("Value must be in range [0, 100]"))
    } else {
      Valid
    }
  }

  implicit val intFormatter = new Formatter[Int] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
      scala.util.control.Exception.allCatch
        .either(data(key).toInt)
        .left.map(e => Seq(FormError(key, "Should be integer")))

    override def unbind(key: String, value: Int): Map[String, String] = ???
  }

  val propForm = Form(
    mapping(
      IntProp.KEY -> nonEmptyText,
      IntProp.VALUE -> of(intFormatter).verifying(valueConstraint)
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
