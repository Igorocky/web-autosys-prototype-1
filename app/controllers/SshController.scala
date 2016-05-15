package controllers

import javax.inject._

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.dao.{User, UserObj, UsersDao}
import services.ssh.{SshConnectionTag, SshService}

import scala.concurrent.ExecutionContext

object AddUserFormObj {
  val NAME = "name"
}

case class AddUserForm(name: String)

@Singleton
class SshController @Inject()(sshService: SshService, usersDao: UsersDao)(
  implicit exec: ExecutionContext,
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  val log: Logger = Logger(this.getClass())

  val addUserForm = Form(
    mapping(
      AddUserFormObj.NAME -> nonEmptyText(maxLength = UserObj.NAME_MAX_LENGTH)
    )(AddUserForm.apply)(AddUserForm.unapply)
  )

  def date = Action.async {
    sshService.executeCommand(SshConnectionTag("localhost-tag"), "date").map(r => Ok(views.html.ssh.date(r)))
  }

  def printSchema = Action {
    Ok(views.html.ssh.schema(usersDao.getSchema))
  }

  def users = Action {
    Ok(views.html.ssh.users(List()))
  }

  def add = Action.async(
    parse.form(
      addUserForm,
      onErrors = (formWithErrors: Form[AddUserForm]) => {
        log.error(formWithErrors.errors.mkString)
        BadRequest(views.html.ssh.users(List()))
      }
    )
  ){request =>
    usersDao.getUserByName(request.body.name).map{userOpt =>
      if (userOpt.isEmpty) {
        usersDao.addUser(User(None, request.body.name))
        Redirect(routes.SshController.users)
      } else {
        log.error("User already exists")
        BadRequest(views.html.ssh.users(List()))
      }
    }
  }
}
