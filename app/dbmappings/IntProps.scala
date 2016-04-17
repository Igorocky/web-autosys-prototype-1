package dbmappings

import slick.driver.H2Driver.api._

class IntProps(tag: Tag) extends Table[(String, Int)](tag, "INTPROPS"){
  def key = column[String]("KEY", O.PrimaryKey)
  def value = column[Int]("VALUE")

  def * = (key, value)
}
