package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Sink.head
import akka.stream.scaladsl.Source.fromFuture
import akka.stream.scaladsl._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.dao.{User, UserObj, UsersDao}
import services.ssh.{SshConnectionTag, SshService}

object AddUserFormObj {
  val NAME = "name"
}

case class AddUserForm(name: String)

@Singleton
class SshController @Inject()(sshService: SshService, usersDao: UsersDao)
                             (val messagesApi: MessagesApi,
                              implicit private val actorSystem: ActorSystem) extends Controller with I18nSupport {

  private val log: Logger = Logger(this.getClass())
  private implicit val materializer = ActorMaterializer()

  val addUserForm = Form(
    mapping(
      AddUserFormObj.NAME -> nonEmptyText(maxLength = UserObj.NAME_MAX_LENGTH)
    )(AddUserForm.apply)(AddUserForm.unapply)
  )

  def date = Action.async {
    fromFuture(sshService.executeCommand(SshConnectionTag("localhost-tag"), "date"))
      .map(r => Ok(views.html.ssh.date(r)))
      .runWith(head)
  }

  def printSchema = Action {
    Ok(views.html.ssh.schema(usersDao.getSchema))
  }

  def users = Action.async {
    usersDao.getAllUsers.map(us => Ok(views.html.ssh.users(us))).runWith(head)
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
    fromFuture(usersDao.getUserByName(request.body.name)).flatMapConcat{
      case None =>
        fromFuture(usersDao.addUser(User(None, request.body.name)))
          .map(_ => Redirect(routes.SshController.users))
      case _ =>
        Source.single(log.error("User already exists"))
          .flatMapConcat(_ => usersDao.getAllUsers)
          .map(users => BadRequest(views.html.ssh.users(users)))
    }.runWith(head)
  }

  def delete(id: Long) = Action.async{
    fromFuture(usersDao.deleteUser(id)).map(_ => Redirect(routes.SshController.users)).runWith(head)
  }
}
