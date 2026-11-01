import cats.Functor
import cats.syntax.all.*

object Functors extends App {

  ////////////////////////////////////Recap: the map we already know

  //Informally, a functor is anything with a map method.
  //We already know lots of types that have one: List, Option, Either, Future...
  //so list is a funtor, and it has it's own map from standard library
  
  println(List(1, 2, 3).map(n => n + 1)) // List(2, 3, 4)

  //Similarly, when we map over an Option we transform the contents but leave the
  //Some or None context unchanged. Same for Either with its Left and Right contexts.

  println(Option(1).map(n => n + 1)) // Some(2)
  println(Option.empty[Int].map(n => n + 1)) // None

  //Because map leaves the structure of the context unchanged, we can call it
  //repeatedly to sequence multiple computations

  println(List(1, 2, 3).map(n => n + 1).map(n => n * 2).map(n => s"${n}!")) // List(4!, 6!, 8!)

  //So we should think of map not as an iteration pattern, but as a way of sequencing
  //computations on values, ignoring some complication dictated by the data type:
  //• Option - the value may or may not be present
  //• Either - there may be a value or an error
  //• List   - there may be zero or more values
  //• Future - the value may not be there yet

  //Example from our code, the Either case: toJson only deals with the happy path (an amount).
  //map runs it on a Right and skips it on a Left, so toJson never has to think about errors

  //pretend we have a response from a downstream here amd we want tp do something o it
  def toJson(amount: BigDecimal): String = s"""{"winterFuelPaymentAmount":$amount}"""

  //now we pretend 2 different responses actually successful one failiure
  val found: Either[String, BigDecimal]    = Right(BigDecimal(215.67))
  val notFound: Either[String, BigDecimal] = Left("NOT_FOUND")

  // so we dont know what found will be yet because all we know its that its an either, so we  map the either using tojson as function
  println(found.map(amount => toJson(amount))) // Right({"winterFuelPaymentAmount":215.67})
  println(notFound.map(amount => toJson(amount))) // Left(NOT_FOUND)   toJson never runs

  ////////////////////////////////////Two different maps

  //Careful: everything so far is the map that List, Option and Either have on their own.
  
  //It's part of the Scala standard library, there is no Cats in it yet.
  
  //Cats Functors have another map that does the same thing. , called differently:

  println(List(1, 2, 3).map(n => n + 1)) // List(2, 3, 4)   List's own method
  println(Functor[List].map(List(1, 2, 3))(n => n + 1)) // List(2, 3, 4)   the method of Cats' Functor[List]

  ////////////////////////////////////A functor is a pattern, not a Cats thing

  //The pattern is:
  //• a type that holds something else like List or Option 
  //• a map that changes the values inside and nothing else
  
  //• functors in the standard library: List, Vector, Option, Either, Try, Future, all the collections...
  //• functors that come with Cats: Validated, NonEmptyList, Chain, Eval, EitherT...
  //a nd functors we can build

  
  //////////lets build one
  final case class HttpResponse[A](status: Int, body: A)

  object HttpResponse {
    given Functor[HttpResponse] with
      def map[A, B](fa: HttpResponse[A])(f: A => B): HttpResponse[B] =
        HttpResponse(fa.status, f(fa.body))
  }

  val response = HttpResponse(200, BigDecimal(215.67))
  println(response.map(toJson)) // HttpResponse(200,{"winterFuelPaymentAmount":215.67})

  ////////////////////////////////////What F[_] means



  ////////////////////////////////////The Functor signature

  //Here is a simplified version of the definition from Cats

  // trait Functor[F[_]] {
  //   def map[A, B](fa: F[A])(f: A => B): F[B]
  // }

  //Piece by piece:
  //• Functor[F[_]]  the type class, for some type with a hole F: List, Option...
  //• map[A, B]      two placeholders: A is the type of the values before, B the type of the values after
  //• fa: F[A]       what we start with, for example a List[Int]
  //• f: A => B      the function applied to each value, for example Int => String
  //• : F[B]         what we get back, for example a List[String]

  //F[A] => F[B] does not mean the type has to change. B is whatever f returns: a new type or the same one.
  //What never changes is F: List in, List out. Option in, Option out
  val numbers: List[Int] = List(1, 2, 3)
  
  println(Functor[List].map(numbers)(n => s"#$n")) // List(#1, #2, #3)          A = Int, B = String
  println(Functor[List].map(numbers)(n => n * 2)) // List(2, 4, 6)             A = Int, B = Int

  //this works because...
  //Cats already has a Functor[List] (catsStdInstancesForList). Simplified, it looks like this.
  //It lives in its own object so it doesn't replace the real one in the rest of the file

  object SimplifiedListFunctor {
    given Functor[List] with
      def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }

  ////////////////////////////////////Functor laws

  //Like monoids, functors must obey laws. In plain words: map only changes the values, never the structure.
  //but as with monoids we can write cats Functpr instances that dont follow that and it will still compile, 
  // just wouldnt give us confidecnce  that our code will be doing what its supposed to


  ////////////////////////////////////Functors in Cats

  //Once again: the type class, the instances, and the syntax.

  //////////Functor type class and instances

  //Same pattern as Monoid: Functor (the companion object) has an apply that gives us the instance.
  
  //there are Instances for List, Option, Either, Future... are in scope with no extra imports

  val list1   = List(1, 2, 3)
  val option1 = Option(123)

  println(Functor[List].map(list1)(_ * 2)) // List(2, 4, 6)
  println(Functor[Option].map(option1)(_.toString)) // Some(123)

  //Besides map, the type class has some helpers. All of them are built on map

  //lift turns a function on plain values (Int => Int) into one on Options (Option[Int] => Option[Int]).

  val func: Int => Int = x => x + 1
  val liftedFunc: Option[Int] => Option[Int] = Functor[Option].lift(func)

  println("-------------------------3--------------------------3-----------------------------------3")
  println("lifdted" + List(Option(1), None, Option(3)).map(liftedFunc)) // List(Some(2), None, Some(4))


  //println("plain" + List(Option(1), None, Option(3)).map(func)) // 
  
  
  println("-------------------------3--------------------------3-----------------------------------3")
  //as replaces every value with the same one, keeping the structure

  println(Functor[List].as(list1, "As")) // List(As, As, As)

  //////////Functor syntax

  //With import cats.syntax.all.* those helpers become methods on the value itself

  println(list1.as("As")) // List(As, As, As)


  //The syntax also adds map, but for List and Option we never see it: when a type has its own map,
  //Scala always uses that one. The syntax map earns its place in generic code, like doMath

  //doMath is one method that works for any F that has a Functor, without knowing what F is:
  //• [F[_]]                        F is some type with a hole: Option, List, Either...
  //• start: F[Int]                 an F with Ints inside: Option[Int], List[Int]...
  //• (using functor: Functor[F])   the caller has to prove F has a map. The compiler finds the proof
  //                                (an instance from Cats, or one of ours) and passes it in for us
  //• start.map(...)                F has no map of its own that we know about, so this is the syntax map,
  //                                which calls functor.map(start)(...)
  //Same idea as addAll in the Monoids lesson, with map instead of combine

  def doMath[F[_]](start: F[Int])
                  (using functor: Functor[F]): F[Int] =
    start.map(n => (n + 1) * 2)

  println(doMath(Option(20))) // Some(42)
  println(doMath(List(1, 2, 3))) // List(4, 6, 8)
  println(doMath(Right(20): Either[String, Int])) // Right(42)

  ////////////////////////////////////Where this is going

  //In our services the values are usually wrapped twice: Future[Either[Error, A]]
  //To reach the value we have to map inside a map

  val nested: List[Option[Int]] = List(Some(1), None, Some(3))

  println(nested.map(option => option.map(n => n + 1))) // List(Some(2), None, Some(4))
  
  //another way is with compose
  
  println(Functor[List].compose[Option].map(nested)(n => n + 1)) // List(Some(2), None, Some(4))
  
  //A map inside a map is not a flatMap: it keeps both layers. flatMap merges them,
  //so the Options and the None are gone

  println(nested.flatMap(option => option.map(n => n + 1))) // List(2, 4)

  //Functors compose, so Cats can build one map that goes through both layers.



  //Next: map can only apply plain functions A => B. 
  // When the next step can also fail,
  //or is also a Future (A => F[B]), we need flatMap. That is a Monad.
  //And one flatMap going through two layers, Future and Either, is what EitherT gives us.
}
