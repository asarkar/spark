package org.abhijitsarkar.ufo

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.scaladsl.Consumer
import com.typesafe.config.Config
import org.abhijitsarkar.ufo.consumer.Heartbeat

import scala.concurrent.Await
import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
class ConsumerTerminator(val config: Config) extends Actor with ActorLogging {
  private[this] val consumerActive = new AtomicBoolean(false)
  private[this] val consumerCtl = new AtomicReference[Consumer.Control]()
  private[this] val analytics = new AtomicReference[java.util.Map[String, java.util.Map[String, Int]]]()

  import scala.concurrent.duration._

  private[this] val batchDurationMillis = Try(config.getLong("batchDurationMillis"))
    .getOrElse(30000L)
    .milliseconds

  private[this] val terminationTimeoutMillis = Try(config.getLong("terminationTimeoutMillis"))
    .getOrElse(2 * 60 * 1000L)

  private[this] val start = System.currentTimeMillis

  private[this] val system = context.system
  system.scheduler
    .schedule(batchDurationMillis, batchDurationMillis, new Runnable {
      override def run = {
        if (!consumerActive.compareAndSet(true, false)) {
          log.warning("Detected idle consumer!")

          if (System.currentTimeMillis - start > terminationTimeoutMillis) {
            log.warning("Stopping consumer.")

            PrettyPrinter.print(analytics.get)
            Await.result(consumerCtl.get.shutdown.flatMap(_ => system.terminate)(system.dispatcher),
              1.second)
          }
        }
      }
    })(system.dispatcher)

  override def receive: Receive = {
    case Heartbeat(ctl, analytics) => {
      this.consumerActive.set(true)
      this.consumerCtl.set(ctl)
      this.analytics.set(analytics)
    }
  }
}

object ConsumerTerminator {
  def props(config: Config) = Props(new ConsumerTerminator(config))
}
