package org.abhijitsarkar.ufo.producer

import akka.actor.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

/**
  * @author Abhijit Sarkar
  */
trait ActorContext {
  implicit def system: ActorSystem

  implicit def executionContext: ExecutionContext

  implicit def materializer: Materializer
}
