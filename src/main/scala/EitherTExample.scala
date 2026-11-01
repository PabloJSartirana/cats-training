import cats.data.EitherT
import cats.syntax.all.*
import play.api.libs.json.{JsValue, Json, Reads}

import java.time.LocalDate
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object EitherTExample {

  //At the end of the Monads lesson we had Future[Either[Error, A]] and a pattern match at every step.
  //EitherT fixes that. Here is a simplified version of the definition from Cats

  // final case class EitherT[F[_], E, A](value: F[Either[E, A]]) {
  //   def map[B](f: A => B): EitherT[F, E, B]
  //   def flatMap[B](f: A => EitherT[F, E, B]): EitherT[F, E, B]
  // }

  //It is only a wrapper around F[Either[E, A]], with a map and a flatMap that go through both layers at once.
  //We read the type inside out: EitherT[Future, ApiError, A] wraps a Future[Either[ApiError, A]]
  //It is called a monad transformer because it gives another monad (Future here) the behaviour of Either:
  //stop at the first Left

  //The example: an endpoint that returns the winter fuel payment of a taxpayer for a tax year
  //1. parse and validate the JSON body              sync, can fail           Either
  //2. look up the taxpayer by UTR                   async, can fail          Future[Either]
  //3. check they are eligible                       a business rule          Boolean
  //4. look up the payment for their NINO and year   async, may be missing    Future[Option]
  //5. send an audit event                           async, can't fail        Future
  //Five steps, four different shapes. The connectors are fake so it runs without a server

  ////////////////////////////////////The domain

  final case class PaymentRequest(utr: String, taxYear: String)

  object PaymentRequest {
    given Reads[PaymentRequest] = Json.reads[PaymentRequest]
  }

  final case class Taxpayer(utr: String, nino: String, name: String, dateOfBirth: LocalDate)

  //What a connector gives back when the HTTP call fails (in a Play service, something like UpstreamErrorResponse)
  final case class UpstreamError(statusCode: Int, message: String)

  //Everything our endpoint can fail with. An enum instead of Strings: we can pattern match on it,
  //and the compiler warns us if we forget a case
  enum ApiError {
    case InvalidRequest(reason: String)
    case TaxpayerNotFound(utr: String)
    case NotEligible(utr: String)
    case PaymentNotFound(utr: String, taxYear: String)
    case DownstreamError(service: String, statusCode: Int)
  }

  ////////////////////////////////////Fake connectors

  //Each one prints the call it makes, so we can see which steps ran

  object TaxpayerConnector {
    private val taxpayers = Map(
      "1234567890" -> Taxpayer("1234567890", "AB123456C", "Margaret", LocalDate.of(1950, 3, 14)),
      "2222222222" -> Taxpayer("2222222222", "CD654321A", "Tom", LocalDate.of(1990, 7, 2)),
      "3333333333" -> Taxpayer("3333333333", "EF111111B", "Ann", LocalDate.of(1955, 1, 1))
    )

    def getTaxpayer(utr: String): Future[Either[UpstreamError, Taxpayer]] = Future {
      println(s"    taxpayer-details  GET /taxpayer/$utr")
      if (utr == "9999999999") Left(UpstreamError(503, "Service Unavailable"))
      else taxpayers.get(utr).toRight(UpstreamError(404, s"No taxpayer with UTR $utr"))
    }
  }

  object PaymentsConnector {
    private val payments = Map(
      ("AB123456C", "2025-26") -> BigDecimal(300),
      ("AB123456C", "2024-25") -> BigDecimal(200),
      ("EF111111B", "2024-25") -> BigDecimal(200)
    )

    //A missing payment is not an error for this connector, so it returns an Option
    def getPayment(nino: String, taxYear: String): Future[Option[BigDecimal]] = Future {
      println(s"    payments          GET /payments/$nino/$taxYear")
      payments.get((nino, taxYear))
    }
  }

  object AuditConnector {
    //Fire and forget: from our point of view it can't fail
    def sendEvent(event: String): Future[Unit] = Future {
      println(s"    audit             POST $event")
    }
  }

  ////////////////////////////////////Plain functions that return Either, like in the Monads lesson

  private val TaxYear = """(\d{4})-(\d{2})""".r

  def parseRequest(body: String): Either[ApiError, PaymentRequest] =
    Either.catchNonFatal(Json.parse(body))
      .leftMap(_ => ApiError.InvalidRequest("body is not valid JSON"))
      .flatMap { json =>
        json.validate[PaymentRequest].asEither
          .leftMap(errors => ApiError.InvalidRequest(s"missing or invalid ${errors.map(_._1).mkString(", ")}"))
      }

  def validateUtr(utr: String): Either[ApiError, String] =
    Either.cond(utr.matches("[0-9]{10}"), utr, ApiError.InvalidRequest(s"invalid UTR $utr"))

  def validateTaxYear(taxYear: String): Either[ApiError, String] =
    taxYear match {
      case TaxYear(start, end) if (start.toInt + 1) % 100 == end.toInt => Right(taxYear)
      case _                                                           => Left(ApiError.InvalidRequest(s"invalid tax year $taxYear"))
    }

  //Made-up rule for the demo
  def isEligible(taxpayer: Taxpayer): Boolean =
    taxpayer.dateOfBirth.isBefore(LocalDate.of(1960, 1, 1))

  //Connector errors are not our API errors, so we translate them
  def fromUpstream(utr: String)(error: UpstreamError): ApiError =
    error.statusCode match {
      case 404    => ApiError.TaxpayerNotFound(utr)
      case status => ApiError.DownstreamError("taxpayer-details", status)
    }

  def toJson(taxpayer: Taxpayer, taxYear: String, amount: BigDecimal): JsValue =
    Json.obj("name" -> taxpayer.name, "taxYear" -> taxYear, "winterFuelPaymentAmount" -> amount)

  ////////////////////////////////////Without EitherT

  //The sync part is fine, a for comprehension over Either. The trouble starts with the first Future:
  //every step needs a pattern match only to pass the error along, and the code keeps moving to the right

  object WithoutEitherT {

    def getPayment(body: String): Future[Either[ApiError, JsValue]] = {
      val validated: Either[ApiError, (String, String)] =
        for {
          request <- parseRequest(body)
          utr     <- validateUtr(request.utr)
          taxYear <- validateTaxYear(request.taxYear)
        } yield (utr, taxYear)

      validated match {
        case Left(error)           => Future.successful(Left(error))
        case Right((utr, taxYear)) =>
          TaxpayerConnector.getTaxpayer(utr).flatMap {
            case Left(upstreamError)                      => Future.successful(Left(fromUpstream(utr)(upstreamError)))
            case Right(taxpayer) if !isEligible(taxpayer) => Future.successful(Left(ApiError.NotEligible(utr)))
            case Right(taxpayer)                          =>
              PaymentsConnector.getPayment(taxpayer.nino, taxYear).flatMap {
                case None         => Future.successful(Left(ApiError.PaymentNotFound(utr, taxYear)))
                case Some(amount) =>
                  AuditConnector.sendEvent(s"winter-fuel-payment-viewed utr=$utr taxYear=$taxYear").map { _ =>
                    Right(toJson(taxpayer, taxYear, amount))
                  }
              }
          }
      }
    }
  }

  ////////////////////////////////////With EitherT

  //The only real work is getting each step into the same shape, EitherT[Future, ApiError, _].
  //This is most of what we need day to day:
  //
  //  the step returns            lift it with
  //  Future[Either[E, A]]        EitherT(fea)
  //  Either[E, A]                EitherT.fromEither[Future](ea)
  //  Future[Option[A]]           EitherT.fromOptionF(foa, error)
  //  Option[A]                   EitherT.fromOption[Future](oa, error)
  //  Future[A] (can't fail)      EitherT.liftF(fa)
  //  A (a plain value)           EitherT.pure[Future, E](a)
  //  a rule (Boolean)            EitherT.cond[Future](test, a, error)
  //  an error                    EitherT.leftT[Future, A](error)
  //
  //  and on the way out
  //  .leftMap(f)                 change the error, e.g. connector errors into our errors
  //  .value                      back to Future[Either[E, A]]
  //  .fold(f, g)                 Future[B], one function for each side
  //
  //The [Future] in fromEither[Future] is there because nothing in the argument tells the compiler which F we want

  object WinterFuelPaymentService {

    def getPayment(body: String): Future[Either[ApiError, JsValue]] = {
      val result: EitherT[Future, ApiError, JsValue] =
        for {
          request  <- EitherT.fromEither[Future](parseRequest(body))
          utr      <- EitherT.fromEither[Future](validateUtr(request.utr))
          taxYear  <- EitherT.fromEither[Future](validateTaxYear(request.taxYear))
          taxpayer <- EitherT(TaxpayerConnector.getTaxpayer(utr)).leftMap(fromUpstream(utr))
          _        <- EitherT.cond[Future](isEligible(taxpayer), (), ApiError.NotEligible(utr))
          amount   <- EitherT.fromOptionF(PaymentsConnector.getPayment(taxpayer.nino, taxYear), ApiError.PaymentNotFound(utr, taxYear))
          _        <- EitherT.liftF(AuditConnector.sendEvent(s"winter-fuel-payment-viewed utr=$utr taxYear=$taxYear"))
        } yield toJson(taxpayer, taxYear, amount)

      //EitherT is glue inside the method. The method returns the plain Future[Either], so callers
      //don't need to know about it (the book's advice for larger code bases)
      result.value
    }
  }

  ////////////////////////////////////The controller

  //A stand-in for Play's Result so the example runs without a server
  final case class Response(status: Int, body: JsValue)

  def errorJson(code: String, message: String): JsValue =
    Json.obj("code" -> code, "message" -> message)

  //One place that turns every error into a response
  def toResponse(error: ApiError): Response =
    error match {
      case ApiError.InvalidRequest(reason)           => Response(400, errorJson("INVALID_REQUEST", reason))
      case ApiError.TaxpayerNotFound(utr)            => Response(404, errorJson("TAXPAYER_NOT_FOUND", s"No taxpayer with UTR $utr"))
      case ApiError.NotEligible(utr)                 => Response(422, errorJson("NOT_ELIGIBLE", s"Taxpayer $utr is not eligible"))
      case ApiError.PaymentNotFound(utr, taxYear)    => Response(404, errorJson("PAYMENT_NOT_FOUND", s"No payment for $utr in $taxYear"))
      case ApiError.DownstreamError(service, status) => Response(502, errorJson("DOWNSTREAM_ERROR", s"$service returned $status"))
    }

  //The controller gets a plain Future[Either]: map from the Functors lesson and fold are all it needs
  def getWinterFuelPayment(body: String): Future[Response] =
    WinterFuelPaymentService.getPayment(body).map(_.fold(toResponse, json => Response(200, json)))

  ////////////////////////////////////Things that catch people out

  //• A failed Future is not a Left. EitherT only sees what is inside the Future, so an exception
  //  (a timeout, a refused connection) still comes out as a failed Future. Recover it in the connector
  //  if it should be a Left (see the end of main)
  //• Each step waits for the one before. That's what we want when a step needs the previous result
  //  (we need the NINO to find the payment), but independent calls also run one after the other
  //• The first Left wins. To collect all the validation errors at once we need Validated (Monoids lesson)
  //• When the compiler can't work out the types, it's usually a missing [Future] in fromEither, cond, pure...

  ////////////////////////////////////Run it

  def main(args: Array[String]): Unit = {

    val requests = List(
      "Happy path"           -> """{"utr":"1234567890","taxYear":"2025-26"}""",
      "Body is not JSON"     -> """utr=1234567890&taxYear=2025-26""",
      "Missing field"        -> """{"utr":"1234567890"}""",
      "Invalid tax year"     -> """{"utr":"1234567890","taxYear":"2025-27"}""",
      "Unknown taxpayer"     -> """{"utr":"0000000000","taxYear":"2025-26"}""",
      "Not eligible"         -> """{"utr":"2222222222","taxYear":"2025-26"}""",
      "No payment that year" -> """{"utr":"3333333333","taxYear":"2025-26"}""",
      "Downstream is down"   -> """{"utr":"9999999999","taxYear":"2025-26"}"""
    )

    //Look at the connector calls under each request: after the first Left nothing else runs

    requests.foreach { (label, body) =>
      println(s"--- $label: $body")
      val response = Await.result(getWinterFuelPayment(body), 1.second) //Await only so the demo can print
      println(s"    => ${response.status} ${Json.stringify(response.body)}")
    }

    //                        connector calls                       response
    // Happy path             taxpayer-details, payments, audit     200 {"name":"Margaret","taxYear":"2025-26","winterFuelPaymentAmount":300}
    // Body is not JSON       none                                  400 INVALID_REQUEST body is not valid JSON
    // Missing field          none                                  400 INVALID_REQUEST missing or invalid /taxYear
    // Invalid tax year       none                                  400 INVALID_REQUEST invalid tax year 2025-27
    // Unknown taxpayer       taxpayer-details                      404 TAXPAYER_NOT_FOUND
    // Not eligible           taxpayer-details                      422 NOT_ELIGIBLE
    // No payment that year   taxpayer-details, payments            404 PAYMENT_NOT_FOUND
    // Downstream is down     taxpayer-details                      502 DOWNSTREAM_ERROR taxpayer-details returned 503

    println("--- Without EitherT: same answer, more code")
    println(Await.result(WithoutEitherT.getPayment(requests.head._2), 1.second))

    println("--- A failed Future is not a Left")
    val timedOut: EitherT[Future, ApiError, BigDecimal] = EitherT(Future.failed(new RuntimeException("Read timed out")))
    println(Await.ready(timedOut.value, 1.second).value) // Some(Failure(java.lang.RuntimeException: Read timed out))
  }

}
