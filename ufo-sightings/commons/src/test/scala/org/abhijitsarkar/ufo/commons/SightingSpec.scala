package org.abhijitsarkar.ufo.commons

import java.time.{Month, YearMonth}

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */

import org.abhijitsarkar.ufo.commons.SightingProtocol._
import spray.json._

class SightingSpec extends FlatSpec with Matchers {
  "Sighting" should "serialize" in {
    val sighting = Sighting(
      eventDateTime = Some("2/9/17 13:05"),
      duration = Some("10 minutes"),
      yearMonth = Some(YearMonth.of(2017, 2))
    ).toJson.compactPrint

    sighting shouldBe ("""{"eventDateTime":"2017-02-09 13:05","duration":"10 minutes"}""")
  }

  it should "parse duration" in {
    val durations = Table(
      "duration",
      "5-6 minutes",
      "6 minutes",
      "1 to 6 minutes",
      "~6 minutes"
    )

    forAll(durations) { x: String =>
      val s = Sighting(duration = Some(x))
      s.duration shouldBe defined
      s.duration.get.getSeconds shouldBe (360L)
    }
  }

  it should "fallback to default duration" in {
    val s = Sighting(duration = Some("junk"))
    s.duration shouldBe None
  }

  it should "parse datetime" in {
    val datetime = Table(
      ("eventDateTime", "yearMonth", "year", "month", "day", "hour", "minute"),
      ("2/9/17", YearMonth.of(2017, 2), 2017, Month.FEBRUARY, 9, 0, 0),
      ("2/9/17 13:05", YearMonth.of(2017, 2), 2017, Month.FEBRUARY, 9, 13, 5)
    )

    forAll(datetime) { (x: String, y: YearMonth, year: Int, month: Month, day: Int, hour: Int, minute: Int) =>
      val s = Sighting(eventDateTime = Some(x), yearMonth = Some(y))
      s.eventDateTime shouldBe defined
      val d = s.eventDateTime.get

      d.getYear shouldBe (year)
      d.getMonth shouldBe (month)
      d.getDayOfMonth shouldBe (day)
      d.getHour shouldBe (hour)
      d.getMinute shouldBe (minute)
    }
  }

  it should "fallback to default datetime" in {
    val s = Sighting(eventDateTime = Some("junk"), yearMonth = Some(YearMonth.of(2017, 2)))
    s.duration shouldBe None
  }
}
