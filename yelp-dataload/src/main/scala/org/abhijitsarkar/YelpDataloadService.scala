package org.abhijitsarkar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */

class YelpDataloadService(val terminationTimeoutMillis: Long,
                          val sc: SparkContext,
                          val sink: RDD[(String, String)] => Unit) {
  val log = Logger.getLogger(classOf[YelpDataloadService])
  val ssc = new StreamingContext(sc, Milliseconds(1000))

  import YelpDataloadService._

  def load(url: String) = {
    log.info("Fetching data from: " + url)

    ssc.receiverStream(HttpReceiver(url))
      .foreachRDD(x => {
        val rdd = x.filter(!_.isEmpty)
          // c.f. http://stackoverflow.com/questions/29295838/org-apache-spark-sparkexception-task-not-serializable
          .map(maybeToTuple)
          .filter(_.isDefined)
          .map(_.get)

        sink(rdd)
      })

    ssc.start
    ssc.awaitTerminationOrTimeout(terminationTimeoutMillis)
  }
}

import scala.collection.JavaConverters._

object YelpDataloadService {
  def apply(terminationTimeoutMillis: Long, sc: SparkContext, sink: RDD[(String, String)] => Unit) =
    new YelpDataloadService(terminationTimeoutMillis, sc, sink)

  private val allowedFields = List("name", "rating", "review_count", "hours", "attributes")

  private def maybeToTuple(content: String) = {
    val objectMapper = new ObjectMapper

    Try(objectMapper.readValue(content, classOf[ObjectNode]))
      .filter(_.has("id"))
      .flatMap(node => {
        val id = node.get("id").textValue

        Try(objectMapper.writeValueAsString(node.retain(allowedFields.asJava)))
          .map(x => Some((id, x)))
      })
      .getOrElse(None)
  }
}

