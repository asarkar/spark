package org.abhijitsarkar.ufo.producer

import java.time.YearMonth

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.scaladsl.Flow
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.domain.Sighting
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

/**
  * @author Abhijit Sarkar
  */
trait Producer {
  self: Crawler =>

  implicit def system: ActorSystem

  def run(producerConfig: Config) = {
    val from = YearMonth.parse(producerConfig.getString("fromYearMonth"))
    val to = YearMonth.parse(producerConfig.getString("toYearMonth"))
    val topic = producerConfig.getString("topic")
    val bootstrapServers = producerConfig.getString("kafka.bootstrap.servers")

    lazy val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)

    import org.abhijitsarkar.ufo.domain.SightingProtocol._
    import spray.json._
    val sink = akka.kafka.scaladsl.Producer.plainSink(producerSettings)
    val flow = Flow[Sighting]
      .map(s => new ProducerRecord[Array[Byte], String](topic, s.toJson.compactPrint))

    sightings(from, to)
      .via(flow)
      .runWith(sink)
  }
}
