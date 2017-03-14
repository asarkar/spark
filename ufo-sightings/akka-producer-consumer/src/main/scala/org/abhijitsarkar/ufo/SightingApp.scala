package org.abhijitsarkar.ufo

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.abhijitsarkar.ufo.consumer.Consumer
import org.abhijitsarkar.ufo.producer.{Crawler, DefaultActorContext, HttpClient, Producer}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
object SightingApp extends App {
  val ac = new DefaultActorContext

  val producer = new Producer with Crawler with HttpClient {
    override implicit def executionContext: ExecutionContext = ac.executionContext

    override implicit def materializer: Materializer = ac.materializer

    override implicit def system: ActorSystem = ac.system
  }

  val consumer = new Consumer {
    override implicit def system: ActorSystem = ac.system

    override implicit def materializer: Materializer = ac.materializer

    override implicit def executionContext: ExecutionContext = ac.executionContext
  }

  val system = ac.system
  val log = system.log
  val config = system.settings.config.getConfig("sighting")

  val f = producer.run(config)

  val ctl = consumer.run(config)

  f.onComplete {
    case Success(_) => log.info("Producer successfully completed.")
    case Failure(t) => log.error(t, "Producer failed!")
  }(ac.executionContext)

  import scala.concurrent.duration._

  val batchDurationMillis = Try(config.getLong("batchDurationMillis"))
    .getOrElse(30000L)
    .milliseconds

  val terminationTimeoutMillis = Try(config.getLong("terminationTimeoutMillis"))
    .getOrElse(2 * 60 * 1000L)

  val start = System.currentTimeMillis
  val counter = new AtomicLong(0)

  system.scheduler
    .schedule(batchDurationMillis, batchDurationMillis, new Runnable {
      override def run = {
        val consumerCounter = consumer.counter.get
        if (consumerCounter <= counter.get) {
          log.warning("Detected idle consumer!")

          if (System.currentTimeMillis - start > terminationTimeoutMillis) {
            log.warning("Stopping consumer.")

            PrettyPrinter.print(consumer.analytics)
            Await.result(ctl.shutdown.flatMap(_ => system.terminate)(ac.executionContext), 1.second)
          }
        } else {
          counter.set(consumerCounter)
        }
      }
    })(ac.executionContext)
}
