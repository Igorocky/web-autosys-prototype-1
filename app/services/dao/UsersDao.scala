package services.dao

import scala.concurrent.Future

trait UsersDao {
  def getSchema: List[String]
  def getUserByName(name: String): Future[Option[User]]
  def addUser(user: User): Future[Int]
}
