object TypeClasses extends App {

  enum Json:
    case JsObject(get: Map[String, Json])
    case JsString(get: String)
    case JsNumber(get: BigDecimal)
    case JsNull

  trait JsonWriter[A]:
    def write(value: A): Json

  //============================ Type Class Instances ============================

  object JsonWriter:
    given JsonWriter[String] with
      def write(value: String): Json =
        Json.JsString(value)


  final case class Person(name: String, age: Int)

  object Person:
    given JsonWriter[Person] with
      def write(value: Person): Json =
        Json.JsObject(Map(
          "name" -> Json.JsString(value.name),
          "age" -> Json.JsNumber(value.age)
        ))

  // ============================ Type class use ============================

  // -------------------------Interface objects --------------------------
  println("-----------------Interface objects------------------------")
  object Json:
    def toJson[A](value: A)(using w: JsonWriter[A]): Json =
      w.write(value)

  println(Json.toJson("A string"))
  println(Json.toJson(Person("Pablo", 36)))


  // ------------------Interface syntax (extension methods)-----------------

  object JsonSyntax:
    extension [A](value: A)
      def toJson(using w: JsonWriter[A]): Json =
        w.write(value)

  import JsonSyntax.*

  println("-----------------Interface syntax------------------------")

  println("An extended String".toJson)
  println(Person("Pablo", 46).toJson)

  // ============================ Type class composition ===========================

//  given optionStringJsonWriter: JsonWriter[Option[String]] with
//    def write(option: Option[String]): Json =
//      option match {
//        case Some(value) => Json.JsString(value)
//        case None => Json.JsNull
//      }
//
//  given optionStringJsonWriter2: JsonWriter[Option[String]] with
//    def write(option: Option[String]): Json =
//      option match {
//        case Some(value) => Json.JsString(value)
//        case None => Json.JsNull
//      }

  given optionWriter[A](using writer: JsonWriter[A]): JsonWriter[Option[A]] with
    def write(option: Option[A]): Json =
      option match {
        case Some(value) => writer.write(value)
        case None => Json.JsNull
      }

  println("-----------------Type class composition------------------------")

  println(Option("An Option string").toJson)
  println(Option.empty[String].toJson)

  println(Option(Person("Maybe Pablo", 46)).toJson)

  // ------------------------------------Summon-------------------------------

//  def summon[A](using instance: A): A = instance

  val summonJsonPersonWriter = summon[JsonWriter[Person]]
  

  println("-----------------Summon------------------------")
  println(summonJsonPersonWriter)
  println(summon[JsonWriter[String]])
}


