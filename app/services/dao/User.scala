package services.dao

import slick.driver.H2Driver.api._

case class User(id: Option[Long], name: String)

object UserObj {
  val NAME_MAX_LENGTH = 50
}

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.Length(UserObj.NAME_MAX_LENGTH))
  def * = (id.?, name) <> (User.tupled, User.unapply)

  def nameIdx = index("USER_NAME_IDX", (name), unique = true)
}

