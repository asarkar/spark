package org.abhijitsarkar.ufo.producer

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.abhijitsarkar.ufo.DefaultActorContext

import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
object ProducerApp extends App {
  private[this] val ac = new DefaultActorContext

  private[this] val producer = new Producer with HtmlScraper with HttpClient {
    override implicit def executionContext: ExecutionContext = ac.executionContext

    override implicit def system: ActorSystem = ac.system

    override implicit def materializer: Materializer = ac.materializer
  }

  private[this] val system = ac.system
  private[this] val log = system.log
  private[this] val config = system.settings.config.getConfig("sighting")

  private[this] implicit val executionContext = ac.executionContext
  private[this] implicit val materializer = ac.materializer
  private[this] val result = producer.runnableGraph(config).run()

  result.onComplete {
    case Success(_) => log.info("Producer successfully completed.")
    case Failure(t) => log.error(t, "Producer failed!")
  }

  private[this] val terminationTimeoutMillis = Try(config.getLong("producer.terminationTimeoutMillis"))
    .getOrElse(2 * 60 * 1000L)

  Await.result(result.flatMap(_ => system.terminate), Duration(terminationTimeoutMillis, MILLISECONDS))
}
