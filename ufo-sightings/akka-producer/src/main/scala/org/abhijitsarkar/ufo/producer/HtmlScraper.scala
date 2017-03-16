package org.abhijitsarkar.ufo.producer

import java.time.YearMonth
import java.time.temporal.ChronoUnit

import akka.NotUsed
import akka.event.Logging
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import akka.stream.{Attributes, Materializer}
import org.abhijitsarkar.ufo.commons.Sighting
import org.jsoup.Jsoup

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */
trait HtmlScraper {
  self: Client =>

  implicit def executionContext: ExecutionContext

  implicit def materializer: Materializer

  private[this] val parallelism = Runtime.getRuntime.availableProcessors * 2

  final def sightings(from: YearMonth, to: YearMonth): Source[Sighting, NotUsed] = {
    val months = from.until(to, ChronoUnit.MONTHS) + 1

    Source.fromIterator(() => Iterator.range(0, months.toInt))
      .map(x => from.plusMonths(x.toLong))
      .log(s"${getClass.getName} - Crawling")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .flatMapMerge(parallelism, self.sightings)
      .mapAsyncUnordered(parallelism)(t => {
        val (response, yearMonth) = (t._1, t._2)
        val body = Unmarshal(response.entity).to[String]
        val status = response.status

        responseMapper(body, yearMonth)(status)
      })
      .mapConcat(_.to[collection.immutable.Seq])
  }

  import scala.collection.JavaConversions._

  private[producer] def responseMapper(body: Future[String], yearMonth: YearMonth):
  PartialFunction[StatusCode, Future[Seq[Sighting]]] = {
    case OK => body.map(Jsoup.parse)
      .map(_.select("table").first)
      .map(t => {
        val rows = t.select("tr")

        rows.map(r => {
          val cols = r.select("td")

          Sighting(
            cols.lift(0).map(_.text),
            cols.lift(3).map(_.text),
            cols.lift(1).map(_.text),
            cols.lift(2).map(_.text),
            cols.lift(4).map(_.text),
            cols.lift(5).map(_.text),
            Some(yearMonth))
        })
      })
    case _ => body.flatMap { entity =>
      Future.failed(new RuntimeException(s"Request failed. Response body: $entity."))
    }
  }
}
