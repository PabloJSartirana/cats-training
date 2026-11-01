import cats.Monoid
import cats.Semigroup
import cats.data.{NonEmptyList, Validated}

import scala.concurrent.duration.DurationInt

object Monoids extends App {
  
// so we pick up from where we left off a few weeks ago and we are going to see a few more concepts 
  
  //STARTING WIT monouids
  // monoids are not exclusive to cats, they are just a programing pattern:  
  
  //they can be represented as a trait with a type, plus an implementation of combine plus an empty element
  
  // in cats it looks something like this simplifies
  
  //  trait Monoid[A] {
  //    def combine(x: A, y: A): A
  //
  //    def empty: A
  //  }
  
  //Monoids, and also Semigroups allow us to add or combine 2 values in a particular way


  //As usual scala and cats provide instances for common types :Ints, Strings, Lists, Options, and many more out of the box from cats library

   //we can defien a monoid for Int in the following way
  
//    given intMonoid: Monoid[Int] with
//      def combine(x: Int, y: Int): Int = x + y
//      def empty: Int = 0
//        
//    println(intMonoid.combine(2, 3) + intMonoid.empty)
  
//    given intMonoid: Monoid[String] with
//      def combine(x: String, y: String): String = x ++ y
//      def empty: String = ""
//        
//    println(intMonoid.combine("2", "3") + intMonoid.empty)  
  
 // ctrl + nn   IntGroup
  //println(Monoid[Int].getClass) ///   or try show instance.....
    //that's also how cats define the monoid for Int
  
  //monoids must formally obey 2 important laws

  def associativeLaw[A](x: A, y: A, z: A)
                       (using m: Monoid[A]): Boolean = {
    m.combine(x, m.combine(y, z)) == m.combine(m.combine(x, y), z)
  }
  
  println(associativeLaw(1, 2, 3))
  println(associativeLaw("1", "2", "3"))
  
  def identityLaw[A](x: A)
                    (using m: Monoid[A]): Boolean = {
    (m.combine(x, m.empty) == x) && (m.combine(m.empty, x) == x)
  }
  println(identityLaw(1))
  //println(associativeLaw("1"))
  
  //question:  can we define an instance of Monoid of Int that implmets combine as a subtraction

  object BadSubtraction {
    given badSubtractionMonoid: Monoid[Int] with
      def combine(x: Int, y: Int): Int = x - y
      def empty: Int = 0
  }

  {
    import BadSubtraction.given
    println(">>>>>>>>>>>>>>>>>>associative minus")
    println(associativeLaw(1, 2, 3))
  }
//why i doesnt work...
  println((1 - 2) - 3)
  println(1 - (2 - 3))
  
  //so all this is to say that, Integer subtraction, for example, is not a valid monoid because subtraction is not associative

  //that means that the compiler wouldnt copmplain but we would be braking the Monoid
  // rule
  // Most of the time we can rely on the instances provided by Cats and assume the library
  // authors knew what they were doing.
  
  //A semigroup is just the combine part of a monoid, without the empty part. 
  // While many semigroups are also monoids, there are some data types for which we cannot define 
  // an empty element.
  
  NonEmptyList
  
  
  // so anothr way to declare monoid is of A : 
  //---------------------------------------------------------------------------------
  //  trait Semigroup[A] {
  //    def combine(x: A, y: A): A
  //  }
  //
  //  trait Monoid[A] extends Semigroup[A] {
  //    def empty: A
  //  }
  //---------------------------------------------------------------------------------
  
  
  //declare Monoids for Boolean
  
  // there are many possible ways to create an instance for boolean, any ideas

  given booleanAndMonoid: Monoid[Boolean] with
    def combine(a: Boolean, b: Boolean) = a && b
    def empty = true
    
  println(booleanAndMonoid.combine(booleanAndMonoid.empty, true) == booleanAndMonoid.combine(true, booleanAndMonoid.empty))
  println(booleanAndMonoid.combine(booleanAndMonoid.empty, false) == booleanAndMonoid.combine(false, booleanAndMonoid.empty))
  
  //  given booleanOrMonoid: Monoid[Boolean] with
//    def combine(a: Boolean, b: Boolean) = a || b
//    def empty = false

  //Now we've seen what monoids are, let's look at their implementation
  // in Cats. Once again we'll look at the three main  aspects of the implementation:
  // the type class, the instances, and the interface.

  //////////////////////////////////////Cats Monoid Instances

  //Cats Monoid follows the standard Cats pattern for the user interface: the
  //companion object has an apply method that returns the type class
  //instance for a particular type
  // so that means we can summon the given instance we have from cats and call its methods

  println(Monoid.apply[String].combine("Hi ", "there"))
  println(Monoid.apply[String].empty)

  //As we know, Monoid extends Semigroup. If we don't need empty
  //we can instead write

  println(Semigroup[String].combine("Hi ", "there"))

  //The standard type class instances for Monoid are all found on the
  //appropriate companion objects, and so are automatically in the
  //given scope with no further imports required.

  ////??$$%^%%&%^&£---------```... QUESTION , WHAT DO WE MENA BU THAT? IS emigroup[String] ACTING AS A COMPANION OBJECT FOR EXAMPLE?
  
  //////////Monoid Syntax

  //import cats.syntax.semigroup.*
  import cats.syntax.all.*

  //Cats provides syntax for the combine method in the form of the |+| operator

  println(1 |+| 2)
  println("Hi " |+| "there" |+| Monoid[String].empty)

  
  //l;ets try to guess the output
  val map1: Map[String, Int] = Map("a" -> 1, "b" -> 2)
  val map2: Map[String, Int] = Map("b" -> 3, "d" -> 4)
  println(map1 |+| map2)

  val tuple1 = ("hello", 123)
  val tuple2 = ("world", 321)
  println(tuple1 |+| tuple2)
  
  println("--- Set: union")
  println(Set(1, 2) |+| Set(2, 3)) // Set(1, 2, 3)
  //We can also write generic code that works with any type for
  //which we have an instance of Monoid


  println("--- Option: combines the contents, None is empty")
  println(Option(1) |+| Option(2)) // Some(3)
  println(Option(1) |+| None) // Some(1)
  println(Option.empty[Int] |+| None) // None
  println("--- Either: Rights combine, the first Left wins")
  val right1: Either[String, Int] = Right(1)
  val right2: Either[String, Int] = Right(2)
  val left1: Either[String, Int] = Left("boom")
  val left2: Either[String, Int] = Left("second")
  println(right1 |+| right2) // Right(3)
  println(right1 |+| left1) // Left(boom)
  println(left1 |+| left2) // Left(boom)
  
  
    //declae addall, as long os we have an instance of MOnoid[A], well take that list, combine items 2 by 2
  
  
  //list monoid
  println(">>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<LIST EMpty")
  println(Monoid[List[Int]].empty)
  
  def addAll[A](values: List[A])
               (using monoid: Monoid[A]): A =
    values.foldRight(monoid.empty)(_ |+| _)
    
    
  /*
    For List(1, 2, 3), A is Int, so it uses Cats’ Monoid[Int] (addition, empty is 0). foldRight goes from right to left:
  3 |+| 0   // 3
  2 |+| 3   // 5
  1 |+| 5   // 6  
   */
  println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + addAll(List(1, 2, 3)))
  
}