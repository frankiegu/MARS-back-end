package com.utamars

import java.sql.Timestamp

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.softwaremill.session.{RefreshTokenStorage, SessionManager}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.utamars.dataaccess._
import com.utamars.util.TimeConversion
import org.joda.time.LocalDate
import spray.json._

import scala.language.implicitConversions

package object api extends AnyRef with TimeConversion with DefaultJsonProtocol with NullOptions with LazyLogging {

  private[api] val config = ConfigFactory.load()

  type Username = String
  type ErrMsg = String
  type Response = ToResponseMarshallable

  // abbreviation
  type SessMgr = SessionManager[Username]
  type RTS = RefreshTokenStorage[Username]

  def halfMonth(m: Int, y: Int, first: Boolean): (LocalDate, LocalDate) = {
    if (first) {  // first half of the month; day 1 to 15.
    val start = new LocalDate(y, m, 1)
      val end   = new LocalDate(y, m, 15)
      (start, end)
    } else {      // second half of the month; day 16 to last day of the month
    val start = new LocalDate(y, m, 16)
      val end   = new LocalDate(y, m, 28).dayOfMonth().withMaximumValue()
      (start, end)
    }
  }

  // needed to be able to convert Map[String, Any] to json using spray
  implicit val anyJsonFormat = new JsonWriter[Any] {
    def write(obj: Any): JsValue = obj match {
      case n: Int           => JsNumber(n)
      case n: Long          => JsNumber(n)
      case n: Short         => JsNumber(n)
      case s: String        => JsString(s)
      case b: Boolean if b  => JsTrue
      case b: Boolean if !b => JsFalse
      case _ => deserializationError("Unsupported type")
    }
  }

  implicit val timestampJsonFormat = new JsonFormat[Timestamp] {
    def write(ts: Timestamp): JsValue = JsNumber(ts.getTime)
    def read(json: JsValue): Timestamp = json match {
      case JsNumber(x) => new Timestamp(x.longValue)
      case x => deserializationError("Expected Timestamp as JsNumber, but got " + x)
    }
  }

  implicit val accJsonFormat    = jsonFormat5(Account.apply)
  implicit val asstJsonFormat   = jsonFormat11(Assistant.apply)
  implicit val instJsonFormat   = jsonFormat4(Instructor.apply)
  implicit val recordJsonFormat = jsonFormat6(ClockInOutRecord.apply)

  implicit class JsonImplicits[T](data: T) {
    def jsonCompat(implicit writer : JsonWriter[T]): String = data.toJson.compactPrint
  }

}
