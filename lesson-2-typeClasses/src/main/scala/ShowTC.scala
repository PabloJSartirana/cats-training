import cats.*
import cats.syntax.all.*

import java.util.Date

object ShowTC extends App {


  // =========================== cats.Show ===========================

//  trait Show[A]:
//    def show(value: A): String


  val showIntInstance: Show[Int] = Show.apply[Int]
  val showStringInstance: Show[String] = Show.apply[String]
  val showBooleanInstance: Show[Boolean] = Show.apply[Boolean]

  println(showIntInstance.show(5))
  println(5.show)

  // =========== Defining a custom Show instance ===========

//  given dateShow: Show[Date] with
//    def show(date: Date): String =
//      s"${date.getTime}ms since the epoch."

  given Show[Date] = Show.show(date => s"${date.getTime}ms since the epoch.")

  println("---------------------------Show-------------------------------")
  println(new Date().show)


}
