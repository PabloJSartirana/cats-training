
object Implicits {

  case class Person(name: String) {
    def greet: String = s"Hello my name is $name"
  }

  implicit class PersonGreeter(name: String) {
    def greet: String = Person(name).greet
  }

  val greetPablo = new PersonGreeter("Pablo")

  greetPablo.greet

  val greetings = "Pablo".greet


  def increment(x: Int)(implicit y: Int) = x + y

  implicit val y: Int = 10

  val increment1 = increment(5)

  def mulitply(x:Int)(implicit t: Int) = x * t


  trait JSONSerializer[T] {
    def toJson(value: T): String
  }

  implicit val personSerializer: JSONSerializer[Person] = new JSONSerializer[Person] {
    override def toJson(person: Person): String =
      s"""
         |{"name" : "${person.name}"}
         |""".stripMargin
  }

  def listToJson[T](list: List[T])(implicit serializer:JSONSerializer[T]):String = {
    list.map(v => serializer.toJson(v)).mkString("[",",","]")
  }

  val persontoJson = listToJson(List(Person("Gilberto"), Person("Sandra")))

  def main(args: Array[String]): Unit = {
    println(greetPablo.greet)
    println(greetings)
    println(increment1)
    println(mulitply(2))
    println(persontoJson)
  }

}
