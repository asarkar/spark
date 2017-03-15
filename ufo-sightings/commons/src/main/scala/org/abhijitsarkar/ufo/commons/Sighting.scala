package org.abhijitsarkar.ufo.commons

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}
import java.util.regex.Pattern

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat}

import scala.util.{Failure, Try}

/**
  * @author Abhijit Sarkar
  */
class Sighting private(val eventDateTime: Option[LocalDateTime], val shape: Option[String], val city: Option[String],
                       val state: Option[String], val duration: Option[Duration], val summary: Option[String])

object Sighting {

  import java.time.YearMonth
  import java.time.format.{DateTimeFormatterBuilder, SignStyle}
  import java.time.temporal.ChronoField

  private def dateTimeFormatter(yearMonth: YearMonth) = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NEVER)
    .appendLiteral('/')
    .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NEVER)
    .appendLiteral('/')
    .appendValueReduced(ChronoField.YEAR, 2, 4, yearMonth.getYear)
    .appendPattern("[ HH:mm]")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .toFormatter

  private def parseDuration(duration: String) = {
    val durationPattern = Pattern.compile("(?<amount>\\d*\\.?\\d+)\\s*(?<unit>[a-zA-Z]+)$")
    import java.time.Duration
    import java.time.temporal.ChronoUnit
    val matcher = durationPattern.matcher(duration)
    (if (matcher.find && matcher.groupCount >= 2) {
      for {
        u <- Try(matcher.group("unit"))
          .map(_.toUpperCase())
          .map(x => if (x.endsWith("S")) x else x + "S")
        unit = ChronoUnit.valueOf(u)
        amount <- Try(matcher.group("amount"))
          .map(_.toLong)
      } yield Duration.of(amount, unit)
    } else {
      Failure(new IllegalArgumentException(s"Failed to parse $duration."))
    })
      .toOption
  }

  val datetimePattern = "yyyy-MM-dd HH:mm"

  def apply(
             eventDateTime: Option[String] = None,
             shape: Option[String] = None,
             city: Option[String] = None,
             state: Option[String] = None,
             duration: Option[String] = None,
             summary: Option[String] = None,
             yearMonth: Option[YearMonth] = None
           ): Sighting = new Sighting(
    (eventDateTime, yearMonth) match {
      case (Some(first), Some(second)) => Try(LocalDateTime.parse(first, dateTimeFormatter(second))).toOption
      case (Some(first), None) => Try(LocalDateTime.parse(first, DateTimeFormatter.ofPattern(datetimePattern))).toOption
      case _ => None
    },
    shape,
    city,
    state,
    duration.flatMap(parseDuration),
    summary
  )
}

import org.abhijitsarkar.ufo.commons.Sighting._
import spray.json._

object SightingProtocol extends DefaultJsonProtocol {

  implicit object SightingJsonFormat extends RootJsonFormat[Sighting] {
    // Some fields are optional so we produce a list of options and
    // then flatten it to only write the fields that were Some(..)
    def write(s: Sighting) = JsObject(
      List(
        s.eventDateTime.map("eventDateTime" -> _.format(DateTimeFormatter.ofPattern(datetimePattern))),
        s.shape.map("shape" -> _),
        s.city.map("city" -> _),
        s.state.map("state" -> _),
        s.duration
          .map(_.toHours)
          .filter(_ > 0)
          .map(_ + " hours")
          .orElse {
            s.duration.map(_.toMinutes)
              .filter(_ > 0)
              .map(_ + " minutes")
          }
          .orElse {
            s.duration.map(_.getSeconds)
              .filter(_ > 0)
              .map(_ + " seconds")
          }
          .map("duration" -> _),
        s.summary.map("summary" -> _)
      )
        .map(_.flatMap(x => Some((x._1, x._2.toJson))))
        .flatten: _*
    )

    def read(value: JsValue) = {
      val jsObject = value.asJsObject
      Sighting(
        jsObject.fields.get("eventDateTime").map(_.convertTo[String]),
        jsObject.fields.get("shape").map(_.convertTo[String]),
        jsObject.fields.get("city").map(_.convertTo[String]),
        jsObject.fields.get("state").map(_.convertTo[String]),
        jsObject.fields.get("duration").map(_.convertTo[String]),
        jsObject.fields.get("summary").map(_.convertTo[String])
      )
    }
  }

}
