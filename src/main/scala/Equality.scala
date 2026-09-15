import cats._
import cats.syntax.all._

import java.util.Date

object Equality extends App {

//  trait Eq[A] {
//    def eqv(x: A, y: A): Boolean
//  }
  println("---------------------------Eq-------------------------------")
  println(2 == 2)
  println(2 =!= 22)
  //println(2 =!= "2")

  val optionList = List(1, 2, 3).map(Option(_))// // List(Some(1), Some(2), Some(3))

  //println(optionList.filter(item => item === 1))

  // ============ Custom equality logic with Eq.instance ============

  implicit val dateEq: Eq[Date] = Eq.instance[Date]((d1, d2) => d1.getTime === d2.getTime)

  val x = new Date()
  Thread.sleep(5)
  val y = new Date()   // a moment later



  println(x === x) // true
  println(x === y) // false (times differ by a fraction of a ms)
}
