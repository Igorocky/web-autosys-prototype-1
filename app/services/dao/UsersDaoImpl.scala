package services.dao

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source.fromPublisher
import akka.stream.scaladsl._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton
class UsersDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit private val actorSystem: ActorSystem) extends UsersDao with HasDatabaseConfigProvider[JdbcProfile]{
  import dbConfig.driver.api._
  val users = TableQuery[Users]

  implicit val materializer = ActorMaterializer()

  override def getUserByName(name: String): Future[Option[User]] =
     fromPublisher(db.stream(users.filter(_.name === name).result)).runWith(Sink.headOption)

  override def addUser(user: User): Future[Int] = db.run(users += user)

  override def getSchema: List[String] = users.schema.create.statements.toList

  override val getAllUsers: Source[List[User], NotUsed] =
    fromPublisher(db.stream(users.result))
      .fold(List[User]())((t, o) => o::t )
      .map(_.reverse)
}