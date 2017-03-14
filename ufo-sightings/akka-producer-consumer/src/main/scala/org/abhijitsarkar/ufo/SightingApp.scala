package org.abhijitsarkar.ufo

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.abhijitsarkar.ufo.consumer.Consumer
import org.abhijitsarkar.ufo.producer.{Crawler, DefaultActorContext, HttpClient, Producer}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * @author Abhijit Sarkar
  */
object SightingApp extends App {
  private[this] val ac = new DefaultActorContext

  private[this] val producer = new Producer with Crawler with HttpClient {
    override implicit def executionContext: ExecutionContext = ac.executionContext

    override implicit def materializer: Materializer = ac.materializer

    override implicit def system: ActorSystem = ac.system
  }

  private[this] val consumer = new Consumer {
    override implicit def system: ActorSystem = ac.system

    override implicit def materializer: Materializer = ac.materializer

    override implicit def executionContext: ExecutionContext = ac.executionContext
  }

  private[this] val system = ac.system
  private[this] val log = system.log
  private[this] val config = system.settings.config.getConfig("sighting")

  private[this] val f = producer.run(config)

  f.onComplete {
    case Success(_) => log.info("Producer successfully completed.")
    case Failure(t) => log.error(t, "Producer failed!")
  }(ac.executionContext)

  private[this] val terminator = system.actorOf(ConsumerTerminator.props(config), "consumer-terminator")
  consumer.run(config, terminator)
}
