package dbmappings

import controllers.IntProp
import slick.driver.H2Driver.api._

object Schema {
  class IntProps(tag: Tag) extends Table[(String, Int)](tag, "INTPROPS"){
    def key = column[String]("KEY", O.PrimaryKey)
    def value = column[Int]("VALUE")

    def * = (key, value)
  }
  val intProps: TableQuery[IntProps] = TableQuery[IntProps]
  def intProp(t: (String, Int)) = IntProp(t._1, t._2)
}


