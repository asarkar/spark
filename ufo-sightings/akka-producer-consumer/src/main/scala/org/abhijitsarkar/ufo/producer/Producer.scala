package org.abhijitsarkar.ufo.producer

import java.time.temporal.{ChronoUnit, Temporal, TemporalAdjuster}
import java.time.{Month, YearMonth}

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ProducerSettings
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.domain.Sighting
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
// c.f. http://doc.akka.io/docs/akka-stream-kafka/current/producer.html
trait Producer {
  self: Crawler =>

  implicit def system: ActorSystem

  def run(config: Config) = {
    val bootstrapServers = Try(config.getString("kafka.bootstrap.servers"))
      .getOrElse("127.0.0.1:9092")

    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)

    import org.abhijitsarkar.ufo.domain.SightingProtocol._
    import spray.json._
    val topic = "ufo"
    val flow = Flow[Sighting]
      .map(s => new ProducerRecord[String, String](topic, s.toJson.compactPrint))

    val sink = akka.kafka.scaladsl.Producer.plainSink(producerSettings)
    batches(config)
      .log(s"${classOf[Producer].getName} - Producing")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .flatMapConcat((sightings _).tupled)
      .via(flow)
      .mergeSubstreams
      .runWith(sink)
  }

  private[producer] def batches(config: Config) = {
    val from = Try(config.getString("producer.fromYearMonth"))
      .map(YearMonth.parse)
      .getOrElse(YearMonth.now().withMonth(Month.JANUARY.getValue()))
    val to = Try(config.getString("producer.toYearMonth"))
      .map(YearMonth.parse)
      .getOrElse(YearMonth.now())
    val batchDurationMillis = Try(config.getLong("batchDurationMillis"))
      .getOrElse(30000L)

    val months = from.until(to, ChronoUnit.MONTHS).toInt + 1
    val batchSize = 12
    val numBatches = scala.math.ceil(months.toDouble / batchSize).toInt

    import scala.concurrent.duration._
    Source(1 to numBatches - 1)
      .zipWith(Source.repeat(1).delay(batchDurationMillis.milliseconds))((x, _) => x)
      .scan(tupleSupplier(true, from, to))((t, i) => tupleSupplier(false, t._1, to))
      .groupBy(100, _._1.getYear)
  }

  private[this] def tupleSupplier(first: Boolean, from: YearMonth, to: YearMonth) = {
    val adjustedFrom = from.`with`(nextYearOrSame(first))
    val adjustedTo = adjustedFrom.`with`(endOfYearOrSame(to))

    (adjustedFrom, adjustedTo)
  }

  private[this] def nextYearOrSame(first: Boolean) = new TemporalAdjuster {
    override def adjustInto(temporal: Temporal): Temporal = {
      val from = YearMonth.from(temporal)
      if (first) from
      else from.withMonth(Month.JANUARY.getValue).plusYears(1)
    }
  }

  private[this] def endOfYearOrSame(to: YearMonth) = new TemporalAdjuster {
    override def adjustInto(temporal: Temporal): Temporal = {
      val from = YearMonth.from(temporal)
      val maybeTo = from.withMonth(Month.DECEMBER.getValue)
      if (maybeTo.isBefore(to)) maybeTo
      else to
    }
  }
}

