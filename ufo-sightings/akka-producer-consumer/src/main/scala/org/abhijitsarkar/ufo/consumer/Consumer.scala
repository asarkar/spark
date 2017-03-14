package org.abhijitsarkar.ufo.consumer

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.function.{BiConsumer, BiFunction}
import java.util.{Map => JavaMap}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{Attributes, Materializer}
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.domain.Sighting
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
case class Heartbeat(ctl: akka.kafka.scaladsl.Consumer.Control,
                     analytics: java.util.Map[String, java.util.Map[String, Int]])

trait Consumer {
  implicit def system: ActorSystem

  implicit def materializer: Materializer

  implicit def executionContext: ExecutionContext

  type Accumulator = JavaMap[String, Int]

  private[this] val parallelism = Runtime.getRuntime.availableProcessors * 2
  private[this] val analytics: JavaMap[String, Accumulator] = new ConcurrentHashMap[String, Accumulator]
  private[this] val counter = new AtomicLong(1)

  def run(config: Config, terminator: ActorRef) = {
    val bootstrapServers = Try(config.getString("kafka.bootstrap.servers"))
      .getOrElse("127.0.0.1:9092")

    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("akka-consumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val batchSize = 12
    val ctl = akka.kafka.scaladsl.Consumer.committableSource(consumerSettings, Subscriptions.topics("ufo"))
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
        offsets.commitScaladsl()
      })
      .toMat(Sink.ignore)(Keep.left)
      .run

    terminator ! Heartbeat(ctl, analytics)
  }

  private[this] def updateAnalytics(s: String) {
    def merge(oldMap: Accumulator, newMap: Accumulator) = {
      newMap.forEach(new BiConsumer[String, Int] {
        override def accept(k: String, v: Int): Unit = {
          oldMap.merge(k, v, new BiFunction[Int, Int, Int] {
            override def apply(i: Int, j: Int): Int = i + j
          })
        }
      })

      oldMap
    }

    def accumulator(k: String) = {
      val m = new ConcurrentHashMap[String, Int]
      m.put(k, 1)

      m
    }

    import org.abhijitsarkar.ufo.domain.SightingProtocol._
    import spray.json._
    val sighting = s.parseJson.convertTo[Sighting]

    val accMerger = new BiFunction[Accumulator, Accumulator, Accumulator] {
      override def apply(t: Accumulator, u: Accumulator): Accumulator = merge(t, u)
    }

    sighting.state
      .foreach(state => {
        analytics.merge("state", accumulator(state.toUpperCase), accMerger)
      })

    sighting.shape
      .foreach(shape => {
        analytics.merge("shape", accumulator(shape.toUpperCase), accMerger)
      })

    sighting.eventDateTime
      .foreach(e => {
        analytics.merge("month", accumulator(e.getMonth.name.toUpperCase), accMerger)
      })

    sighting.eventDateTime
      .foreach(e => {
        analytics.merge("year", accumulator(e.getYear.toString.toUpperCase), accMerger)
      })
  }
}
