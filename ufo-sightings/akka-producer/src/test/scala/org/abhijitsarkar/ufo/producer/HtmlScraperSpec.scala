package org.abhijitsarkar.ufo.producer

import java.nio.charset.StandardCharsets.UTF_8
import java.time.{Month, YearMonth}

import akka.NotUsed
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCode, StatusCodes}
import akka.stream.scaladsl.Source
import org.abhijitsarkar.ufo
import org.abhijitsarkar.ufo.commons.Sighting
import org.scalatest.{AsyncFlatSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Abhijit Sarkar
  */

class HtmlScraperSpec extends ufo.DefaultActorContext with AsyncFlatSpecLike with Matchers {
  def localScraper(statusCode: StatusCode) = new ufo.DefaultActorContext with HtmlScraper with Client {
    override def sightings(yearMonth: YearMonth): Source[(HttpResponse, YearMonth), NotUsed] = {
      val resource = getClass.getResourceAsStream("/ndxe201701.html")
      val html = scala.io.Source.fromInputStream(resource, UTF_8.name()).mkString

      val response = HttpResponse(entity = HttpEntity(html), status = statusCode)

      Source.single((response, YearMonth.of(2017, 1)))
    }
  }

  override implicit val executionContext: ExecutionContext = system.dispatcher

  val remoteScraper = new ufo.DefaultActorContext with HtmlScraper with HttpClient

  "Scraper" should "parse HTML when response OK" in {
    val dt = YearMonth.of(2017, 1)
    val sightings = localScraper(StatusCodes.OK).sightings(dt, dt)
      .runFold(Seq.empty[Sighting])(_ :+ _)

    import org.abhijitsarkar.ufo.commons.SightingProtocol._
    import spray.json._
    sightings.map { s =>
      assert(!s.isEmpty)
      assert(s.filter(_.eventDateTime.isDefined).forall { x =>
        system.log.info(s"Found: ${x.toJson.compactPrint}")
        val dt = x.eventDateTime.get
        dt.getYear == 2017 && dt.getMonth == Month.JANUARY
      })
    }
  }

  it should "parse HTML" in {
    val resource = getClass.getResourceAsStream("/ndxe201701.html")
    val html = scala.io.Source.fromInputStream(resource, UTF_8.name()).mkString

    val sightings = remoteScraper.responseMapper(Future.successful(html), YearMonth.of(2017, 1))(StatusCodes.OK)

    import org.abhijitsarkar.ufo.commons.SightingProtocol._
    import spray.json._
    sightings.map { s =>
      assert(!s.isEmpty)
      assert(s.filter(_.eventDateTime.isDefined).forall { x =>
        system.log.info(s"Found: ${x.toJson.compactPrint}")
        val dt = x.eventDateTime.get
        dt.getYear == 2017 && dt.getMonth == Month.JANUARY
      })
    }
  }

  it should "fail when response not OK" in {
    val dt = YearMonth.of(2017, 1)
    recoverToSucceededIf[RuntimeException] {
      localScraper(StatusCodes.InternalServerError).sightings(dt, dt)
        .runFold(Seq.empty[Sighting])(_ :+ _)
    }
  }

  it should "get all sightings for Feb 2017" in {
    val dt = YearMonth.of(2017, 2)
    val sightings = remoteScraper.sightings(dt, dt)
      .runFold(Seq.empty[Sighting])(_ :+ _)

    sightings.map { s =>
      assert(!s.isEmpty)
      assert(s.filter(_.eventDateTime.isDefined).forall { x =>
        val dt = x.eventDateTime.get
        dt.getYear == 2017 && dt.getMonth == Month.FEBRUARY
      })
    }
  }
}
