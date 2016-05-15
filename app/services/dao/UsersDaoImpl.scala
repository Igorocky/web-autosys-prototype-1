package services.dao

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._

@Singleton
class UsersDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UsersDao with HasDatabaseConfigProvider[JdbcProfile]{
  import dbConfig.driver.api._
  val users = TableQuery[Users]

  override def getUserByName(name: String): Future[Option[User]] =
    db.run(users.filter(_.name === name).result).map(_.headOption)

  override def addUser(user: User): Future[Int] = db.run(users += user)

  override def getSchema: List[String] = users.schema.create.statements.toList
}