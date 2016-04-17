package controllers

import javax.inject.{Inject, Named, Singleton}

import actors.PropsHolderActor._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import dbmappings.IntProps
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import views.html.props._

import scala.concurrent.Await
import scala.concurrent.duration._

case class IntProp(key: String, value: Int)

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

  def allProps = Action(
    Ok(listProps(getAllProps, propForm, propForm))
  )

  def addProp = Action.async(
      parse.form(
        propForm,
        onErrors = (formWithErrors: Form[IntProp]) => BadRequest(listProps(
          allProps = getAllProps,
          addForm = formWithErrors,
          editForm = propForm
        ))
      )
  ){request =>
    (propsHolderActor ? Add(request.body)).map{
      case Success() => Redirect(routes.FormExampleController.allProps)
      case e: Error => BadRequest(listProps(
        allProps = e.props,
        addForm = propForm.fill(request.body),
        editForm = propForm,
        errMsg = Some(e.msg)
      ))
    }
  }

  def removeProp(key: String) = Action.async{request =>
    (propsHolderActor ? Remove(key)).map{
      case Success() => Redirect(routes.FormExampleController.allProps)
      case e: Error => BadRequest(listProps(
        allProps = e.props,
        addForm = propForm,
        editForm = propForm,
        errMsg = Some(e.msg)))
    }
  }

  def prepareEdit(key: String) = Action{request =>
    Ok(listProps(
        allProps = getAllProps,
        addForm = propForm,
        editForm = propForm.fill(getProp(key))
    ))
  }

  def editProp = Action.async(
    parse.form(
      propForm,
      onErrors = (formWithErrors: Form[IntProp]) => BadRequest(listProps(
        allProps = getAllProps,
        addForm = propForm,
        editForm = formWithErrors
      ))
    )
  ){request =>
    (propsHolderActor ? Change(request.body)).map{
      case Success() => Redirect(routes.FormExampleController.allProps)
      case e: Error => BadRequest(listProps(
        allProps = e.props,
        addForm = propForm,
        editForm = propForm.fill(request.body),
        errMsg = Some(e.msg)
      ))
    }
  }

  private def getAllProps = Await.result((propsHolderActor ? GetAll()).map(_.asInstanceOf[List[IntProp]]), 5.seconds)
  private def getProp(key: String) = Await.result((propsHolderActor ? Get(key)).map(_.asInstanceOf[IntProp]), 5.seconds)

  def printSchema = Action{
    import slick.driver.H2Driver.api._
    val intProps = TableQuery[IntProps]
    Ok(intProps.schema.create.statements.map(_.toString).mkString("\n"))
  }
}
