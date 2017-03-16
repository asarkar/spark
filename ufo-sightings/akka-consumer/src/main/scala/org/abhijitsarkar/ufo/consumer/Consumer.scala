package org.abhijitsarkar.ufo.consumer

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{Attributes, Materializer}
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.commons.{AnalyticsAccumulator, Sighting}
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
trait Consumer {
  implicit def system: ActorSystem

  implicit def materializer: Materializer

  implicit def executionContext: ExecutionContext

  private[this] val _analytics: AnalyticsAccumulator = new AnalyticsAccumulator

  def analytics = _analytics.value

  private[this] val parallelism = Runtime.getRuntime.availableProcessors * 2
  val recordsProcessed = new AtomicLong(1)

  def run(config: Config) = {
    val bootstrapServers = Try(config.getString("kafka.bootstrap.servers"))
      .getOrElse("127.0.0.1:9092")

    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("akka-consumer")
      .withProperty(AUTO_OFFSET_RESET_CONFIG, "earliest")

    val batchSize = 12
    akka.kafka.scaladsl.Consumer.committableSource(consumerSettings, Subscriptions.topics("ufo"))
      .log(s"${getClass.getName} - Consuming")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map { msg =>
        updateAnalytics(msg.record.value)
        msg.committableOffset
      }
      .batch(max = batchSize.toLong, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .mapAsync(parallelism)(offsets => {
        system.log.debug("Updating records consumed.")
        recordsProcessed.incrementAndGet
        offsets.commitScaladsl()
      })
      .toMat(Sink.ignore)(Keep.left)
      .run
  }

  import org.abhijitsarkar.ufo.commons.SightingProtocol._
  import spray.json._

  private[this] def updateAnalytics(s: String) {
    Option(s.parseJson.convertTo[Sighting])
      .map(x =>
        (x.state,
          x.shape,
          x.eventDateTime.map(_.getMonth.name),
          x.eventDateTime.map(_.getYear.toString)))
      .foreach(x => {
        x._1.foreach(y => _analytics.add(("state", y)))
        x._2.foreach(y => _analytics.add(("shape", y)))
        x._3.foreach(y => _analytics.add(("month", y)))
        x._4.foreach(y => _analytics.add(("year", y)))
      })
  }
}
