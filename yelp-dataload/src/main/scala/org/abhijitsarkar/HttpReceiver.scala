package org.abhijitsarkar

import java.net.{HttpURLConnection, URL}
import java.util.zip.GZIPInputStream

import org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK
import org.apache.spark.streaming.receiver.Receiver

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * @author Abhijit Sarkar
  */
// https://spark.apache.org/docs/latest/streaming-custom-receivers.html
class HttpReceiver(val url: String) extends Receiver[String](MEMORY_AND_DISK) {
  // Must not block
  override def onStart(): Unit = new Thread(getClass.getSimpleName) {
    receive
  }.start

  private def receive = Try(new URL(url).openConnection().asInstanceOf[HttpURLConnection]) match {
    case Success(conn) => {
      conn.setAllowUserInteraction(false)
      conn.setInstanceFollowRedirects(true)
      conn.setRequestMethod("GET")
      conn.setReadTimeout(60 * 1000)

      val gzipStream = new GZIPInputStream(conn.getInputStream)

      Source.fromInputStream(gzipStream)
        .getLines
        .takeWhile(x => !isStopped && x != null)
        .foreach(store)

      conn.disconnect
    }
    case Failure(t) => stop(t.getMessage, t)
  }

  override def onStop(): Unit = {}
}

object HttpReceiver {
  def apply(url: String) = new HttpReceiver(url)
}
