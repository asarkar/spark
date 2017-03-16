package org.abhijitsarkar.ufo.consumer

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Attributes
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.commons.{AnalyticsAccumulator, Sighting}
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
trait Consumer {
  implicit def system: ActorSystem

  private[this] val _analytics: AnalyticsAccumulator = new AnalyticsAccumulator
  private[this] val _recordsConsumed = new AtomicLong(1)

  def analytics = _analytics.value

  def recordsConsumed = _recordsConsumed.get

  private[this] val parallelism = Runtime.getRuntime.availableProcessors * 2

  def runnableGraph(config: Config) = {
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
        _recordsConsumed.incrementAndGet
        offsets.commitScaladsl()
      })
      .toMat(Sink.ignore)(Keep.left)
  }

  import org.abhijitsarkar.ufo.commons.SightingProtocol._
  import spray.json._

  type MaybeValue = (String, Option[String])

  private[this] def updateAnalytics(s: String) {
    Option(s.parseJson.convertTo[Sighting])
      .map(x =>
        (("state", x.state),
          ("shape", x.shape),
          ("month", x.eventDateTime.map(_.getMonth.name)),
          ("year", x.eventDateTime.map(_.getYear.toString))))
      .foreach(_.productIterator
        .map(_.asInstanceOf[MaybeValue]) // Tuples can be heterogeneous, thus productIterator returns Any
        .foreach(x => x._2.foreach(y => _analytics.add((x._1, y)))))
  }
}
