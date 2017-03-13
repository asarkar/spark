package org.abhijitsarkar.ufo.producer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContext

/**
  * @author Abhijit Sarkar
  */
class DefaultActorContext extends ActorContext {
  override implicit val system: ActorSystem = ActorSystem("ufo-sightings")

  override implicit val executionContext: ExecutionContext = system.dispatcher

  override implicit val materializer: Materializer = ActorMaterializer()
}
