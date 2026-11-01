import cats.Monad
import cats.syntax.all.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object Monads {

  //This lesson uses def main instead of `extends App` because of the Futures:
  //with `extends App` the Futures run while the object is still initialising, and Await times out

  def main(args: Array[String]): Unit = {

    ////////////////////////////////////What is a monad

    //A monad is a type with a flatMap, plus a way to put a plain value into it.
    //The types we already know are monads: Option, Either, List, Future...
    //With a functor we change the value inside using map.
    //With a monad we chain steps where every step returns a new Option, Either, Future...

    ////////////////////////////////////map vs flatMap

    //Both run a function on the value inside. The difference is what that function returns:
    //• map     takes A => B      the function returns a plain value
    //• flatMap takes A => F[B]   the function returns another Option (or Either, List...)
    //Rule of thumb: look at what the function returns, not at the box

    //////////The function returns a plain value: map

    def double(n: Int): Int = n * 2

    println(Option(21).map(double)) // Some(42)

    //Counterexample: flatMap does not compile here. It wants a function that returns an Option, and double returns an Int

    // Option(21).flatMap(double)   // does not compile: Found: Int, Required: Option[Any]

    //////////The function returns an Option: flatMap

    def parseInt(str: String): Option[Int] = str.toIntOption

    println(Option("42").flatMap(parseInt)) // Some(42)
    println(Option("abc").flatMap(parseInt)) // None

    //Counterexample: map compiles, but we get an Option inside an Option

    println(Option("42").map(parseInt)) // Some(Some(42))

    //and the next step breaks, because the value inside is now an Option[Int], not an Int

    // Option("42").map(parseInt).map(n => n + 1)   // does not compile: value + is not a member of Option[Int]

    //That's where the name comes from: flatMap is a map followed by a flatten, which removes one layer

    println(Option("42").map(parseInt).flatten) // Some(42)

    //////////map can't change the box, flatMap can

    //map only changes the value: a Some stays a Some, a Right stays a Right, a List keeps its length.
    //With flatMap the function returns the box, so it can also change it

    def checkPositive(amount: BigDecimal): Either[String, BigDecimal] =
      if (amount > 0) Right(amount) else Left("NEGATIVE_AMOUNT")

    val negative: Either[String, BigDecimal] = Right(BigDecimal(-5))

    println(negative.flatMap(checkPositive)) // Left(NEGATIVE_AMOUNT)   the Right became a Left

    //Counterexample: with map the error ends up inside a Right. It looks like a success!

    println(negative.map(checkPositive)) // Right(Left(NEGATIVE_AMOUNT))

    //Same with List: map always gives back the same number of elements, flatMap can give more or fewer

    println(List(1, 2, 3).map(n => List.fill(n)(n))) // List(List(1), List(2, 2), List(3, 3, 3))   still 3 elements
    println(List(1, 2, 3).flatMap(n => List.fill(n)(n))) // List(1, 2, 2, 3, 3, 3)
    println(List(1, -2, 3).flatMap(n => if (n > 0) List(n) else Nil)) // List(1, 3)   an element can also disappear

    ////////////////////////////////////Chaining steps: fail fast

    //Because flatMap gives back a plain Either, we can chain the next step.
    //If a step fails, the steps after it never run

    val payments = Map("2024-25" -> BigDecimal(200), "2025-26" -> BigDecimal(300))

    def parseTaxYear(raw: String): Either[String, String] =
      if (raw.matches("[0-9]{4}-[0-9]{2}")) Right(raw) else Left(s"INVALID_TAX_YEAR: $raw")

    def findPayment(taxYear: String): Either[String, BigDecimal] =
      payments.get(taxYear).toRight("NOT_FOUND")

    println(parseTaxYear("2025-26").flatMap(findPayment)) // Right(300)
    println(parseTaxYear("2025/26").flatMap(findPayment)) // Left(INVALID_TAX_YEAR: 2025/26)   findPayment never runs
    println(parseTaxYear("2019-20").flatMap(findPayment)) // Left(NOT_FOUND)

    ////////////////////////////////////For comprehensions

    //When a step needs a value from an earlier step, the flatMaps go one inside the other.
    //A for comprehension is the same code written flat: the compiler turns one into the other
    //• every <- is a flatMap
    //• the last <- together with yield is a map

    def paymentWithFlatMap(raw: String): Either[String, String] =
      parseTaxYear(raw).flatMap { taxYear =>
        findPayment(taxYear).map { amount =>
          s"$taxYear: $amount"
        }
      }

    def payment(raw: String): Either[String, String] =
      for {
        taxYear <- parseTaxYear(raw)
        amount  <- findPayment(taxYear)
      } yield s"$taxYear: $amount"

    println(paymentWithFlatMap("2025-26")) // Right(2025-26: 300)
    println(payment("2025-26")) // Right(2025-26: 300)

    //All the <- in one for must be the same monad. To use an Option in an Either for, convert it with toRight

    ////////////////////////////////////Same syntax, different monads

    //The for looks the same with every monad. What happens between the steps is up to the monad:
    //• Option   stops at the first None
    //• Either   stops at the first Left, and keeps the error
    //• List     runs the next step for every value
    //• Future   starts the next step when the previous one has finished

    //List: every combination

    val lookups =
      for {
        nino    <- List("AB123456C", "CD654321A")
        taxYear <- List("2024-25", "2025-26")
      } yield s"$nino $taxYear"

    println(lookups) // List(AB123456C 2024-25, AB123456C 2025-26, CD654321A 2024-25, CD654321A 2025-26)

    //Future: fetchAmount needs the nino, so it only starts once fetchNino has finished

    def fetchNino(utr: String): Future[String] = Future("AB123456C")

    def fetchAmount(nino: String): Future[BigDecimal] = Future(BigDecimal(300))

    val amount: Future[BigDecimal] =
      for {
        nino   <- fetchNino("1234567890")
        amount <- fetchAmount(nino)
      } yield amount

    //Await only so we can print the result here, never in real code
    println(Await.result(amount, 1.second)) // 300

    ////////////////////////////////////Monads in Cats

    //Same story as Functor: Option, Either, List and Future are monads without Cats.
    //Cats adds the Monad type class, so we can write code that works with any monad.
    //It has two methods: pure puts a plain value into the monad, and flatMap

    // trait Monad[F[_]] {
    //   def pure[A](value: A): F[A]
    //   def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
    // }

    println(Monad[Option].pure(3)) // Some(3)
    println(3.pure[List]) // List(3)   the same with the syntax

    //Like doMath in the Functors lesson: one method for any monad

    def sumSquare[F[_]](a: F[Int], b: F[Int])
                       (using monad: Monad[F]): F[Int] =
      for {
        x <- a
        y <- b
      } yield x * x + y * y

    println(sumSquare(Option(3), Option(4))) // Some(25)
    println(sumSquare(List(1, 2), List(10))) // List(101, 104)

    //Cats also adds some helpers for Either (from cats.syntax.all.*)

    println(Either.catchNonFatal("215.67x".toDouble)) // Left(java.lang.NumberFormatException: For input string: "215.67x")
    println(negative.ensure("NEGATIVE_AMOUNT")(amount => amount > 0)) // Left(NEGATIVE_AMOUNT)   flatMap(checkPositive) in one line
    println(findPayment("2019-20").leftMap(error => s"Lookup failed: $error")) // Left(Lookup failed: NOT_FOUND)

    ////////////////////////////////////Summary

    //• map       the function returns a plain value. Only the value changes, never the box
    //• flatMap   the function returns an Option, Either, List, Future... so the box can change too:
    //            Some to None, Right to Left, one element to many or none
    //• for       a chain of flatMaps that ends in a map, written flat
  }

}
