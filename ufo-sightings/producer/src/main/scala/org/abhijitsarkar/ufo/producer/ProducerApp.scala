package org.abhijitsarkar.ufo.producer

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Abhijit Sarkar
  */
object ProducerApp extends App {
  val producer = new ActorContextDefaultImpl with Producer with Crawler with HttpClient

  val as = producer.system
  val log = as.log
  val producerConfig = as.settings.config.getConfig("sighting.producer")

  val f = producer.run(producerConfig)
    .andThen {
      case _ => log.info("Producer completed."); as.terminate
    }(producer.executionContext)

  Await.result(f, 60.seconds)
}
