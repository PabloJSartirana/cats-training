object TypeClasses extends App {

  enum Json:
    case JsObject(get: Map[String, Json])
    case JsString(get: String)
    case JsNumber(get: BigDecimal)
    case JsNull

  trait JsonWriter[A]:
    def write(value: A): Json

  object JsonWriter:
    given JsonWriter[String] with
      def write(value: String): Json =
        Json.JsString(value)

  case class Person(name: String, age: Int)

  object Person:
    given anyname: JsonWriter[Person] with
      def write(value: Person): Json =
        Json.JsObject(Map(
          "name" -> Json.JsString(value.name),
          "age" -> Json.JsNumber(value.age)
        ))

  // ------- 1. Interface objects -------

  object Json:
    def toJson[A](value: A)(using w: JsonWriter[A]): Json =
      w.write(value)

  println(Json.toJson(Person("Dave", 46)))
  println(Json.toJson("A string"))

  // ---- Interface syntax (extension method) ----

  object JsonSyntax:
    extension [A](value: A)
      def toJson(using w: JsonWriter[A]): Json =
        w.write(value)

  // ========================= Type class composition =========================


  given optionStringJsonWriter: JsonWriter[Option[String]] with
    def write(value: Option[String]): Json =
      value match {
        case Some(v) => Json.JsString(v)
        case None => Json.JsNull
      }

  given optionWriter[A](using writer: JsonWriter[A]): JsonWriter[Option[A]] with
    def write(option: Option[A]): Json =
      option match {
        case Some(aValue) => writer.write(aValue)
        case None => Json.JsNull
      }


  // ------- 3. summon -------

  def summon[A](using value: A): A = value

  val jsonPersonWriterInstance = summon[JsonWriter[Person]]
  val summonedJsonStringWriter = summon[JsonWriter[String]]


  import JsonSyntax.*

  println(Person("Dave", 46).toJson)
  println("A string".toJson)
  println(Option("A string").toJson)
  println(Option("A string").toJson)
  println(jsonPersonWriterInstance)
  println(summonedJsonStringWriter)
}


