package org.abhijitsarkar.ufo

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

  val as = ac.system
  val log = as.log
  val config = as.settings.config.getConfig("sighting")

  val f = producer.run(config)

  val ctl = consumer.run(config)

  f.onComplete {
    case Success(_) => log.info("Producer successfully completed.")
    case Failure(t) => log.error(t, "Producer failed!")
  }(ac.executionContext)

  val timeout = Try(config.getLong("terminationTimeoutMillis"))
    .getOrElse(2 * 60 * 1000L)

  // Wait for all messages to be consumed
  Thread.sleep(timeout)

  import scala.concurrent.duration._

  log.warning("Stopping consumer.")
  PrettyPrinter.print(consumer.analytics)
  Await.result(ctl.shutdown.flatMap(_ => as.terminate)(ac.executionContext), timeout.milliseconds)
}
