package org.abhijitsarkar.ufo.producer

import java.time.YearMonth

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.typesafe.config.ConfigFactory
import org.abhijitsarkar.ufo.DefaultActorContext
import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */

class ProducerSpec extends FlatSpec with Matchers {
  val producer = new DefaultActorContext with Producer with Crawler with Client {
    override def sightings(yearMonth: YearMonth): Source[(HttpResponse, YearMonth), NotUsed] = ???
  }

  import scala.collection.JavaConverters._

  "Producer" should "group months from same year" in {
    val producerConfig = ConfigFactory.parseMap(Map[String, String](
      "producer.fromYearMonth" -> "2011-05",
      "producer.toYearMonth" -> "2013-11",
      "producer.batchIntervalMillis" -> "500"
    ).asJava)

    val probe = TestSink.probe[(YearMonth, YearMonth)](producer.system)

    producer.batches(producerConfig)
      .mergeSubstreams
      .runWith(probe)(producer.materializer)
      .request(3)
      .expectNextUnordered(
        (YearMonth.of(2011, 5), YearMonth.of(2011, 12)),
        (YearMonth.of(2012, 1), YearMonth.of(2012, 12)),
        (YearMonth.of(2013, 1), YearMonth.of(2013, 11)))
      .expectComplete
  }
}
