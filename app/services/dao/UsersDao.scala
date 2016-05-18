package services.dao

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait UsersDao {
  def getSchema: List[String]
  def getUserByName(name: String): Future[Option[User]]
  def addUser(user: User): Future[Int]
  def getAllUsers: Source[List[User], NotUsed]
}
