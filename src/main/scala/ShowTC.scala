import Implicits.Person
import cats.*
import cats.syntax.all.*

import java.util.Date

object ShowTC extends App {

  case class Car(model: String, year: Int)
  case class Dog(name: String)

  val showInt: Show[Int] = Show.apply[Int]
  val showString: Show[String] = Show.apply[String]
  val showBoolean: Show[Boolean] = Show.apply[Boolean]

  given carSHowINstance: Show[Car] with
    def show(car: Car): String =
      s"${car.model} from ${car.year.toString}"

  given Show[Date] = Show.show((date => s"${date.getTime}ms since the epoch."))

  given Show[Dog] = Show.fromToString

  println(showInt.show(46))
  println(showString.show("A string"))
  println(showBoolean.show(true))

  println(46.show)
  println(Car("Ferrari", 2026).show)
  println(Dog("Lassie").show)
  println(new Date())

}
