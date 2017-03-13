package org.abhijitsarkar.ufo.producer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
object ProducerApp extends App {
  val producer = new DefaultActorContext with Producer with Crawler with HttpClient

  val as = producer.system
  val log = as.log
  val producerConfig = as.settings.config.getConfig("sighting.producer")

  val f = producer.run(producerConfig)

  f.onComplete(_ match {
    case _ => log.info("Producer completed.")
  })(producer.executionContext)

  val timeout = Try(producerConfig.getLong("terminationTimeoutMillis"))
    .getOrElse(60000L).seconds

  Await.result(f, timeout)
  Await.result(as.terminate, timeout)
}
