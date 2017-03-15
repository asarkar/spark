package org.abhijitsarkar.ufo.consumer

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.abhijitsarkar.ufo.DefaultActorContext
import org.abhijitsarkar.ufo.commons.PrettyPrinter

import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
object ConsumerApp extends App {
  private[this] val ac = new DefaultActorContext

  private[this] val consumer = new Consumer {
    override implicit def system: ActorSystem = ac.system

    override implicit def materializer: Materializer = ac.materializer

    override implicit def executionContext: ExecutionContext = ac.executionContext
  }

  private[this] val system = ac.system
  private[this] val log = system.log
  private[this] val config = system.settings.config.getConfig("sighting")

  private[this] val ctl = consumer.run(config)

  import scala.concurrent.duration._

  private[this] val batchDurationMillis = Try(config.getLong("consumer.livelinessCheckIntervalMillis"))
    .getOrElse(30000L)
    .milliseconds

  private[this] val terminationTimeoutMillis = Try(config.getLong("consumer.terminationTimeoutMillis"))
    .getOrElse(2 * 60 * 1000L)

  private[this] val start = System.currentTimeMillis
  private[this] val recordsConsumed = new AtomicLong(0)
  private[this] implicit val ec = ac.executionContext

  system.scheduler
    .schedule(batchDurationMillis, batchDurationMillis, new Runnable {
      override def run = {
        val consumerRecordsProcessed = consumer.recordsProcessed.get
        if (consumerRecordsProcessed <= recordsConsumed.get) {
          log.warning("Detected idle consumer!")

          if (System.currentTimeMillis - start > terminationTimeoutMillis) {
            log.warning("Stopping consumer.")

            PrettyPrinter.print(consumer.analytics)
            Await.result(ctl.shutdown.flatMap(_ => system.terminate), Duration(terminationTimeoutMillis, MILLISECONDS))
          }
        } else {
          recordsConsumed.set(consumerRecordsProcessed)
        }
      }
    })
}
