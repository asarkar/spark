package org.abhijitsarkar

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */

class YelpHttpToStdoutService(val sparkProperties: SparkProperties) {
  val sc = {
    SparkSession.builder()
      .master(sparkProperties.master)
      .appName("yelp-dataload")
      .getOrCreate
  }.sparkContext
  val ssc = new StreamingContext(sc, Milliseconds(1000))

  import YelpHttpToStdoutService._

  def load(url: String) = {
    ssc.receiverStream(HttpReceiver(url))
      .foreachRDD(
        _.filter(!_.isEmpty)
          // c.f. http://stackoverflow.com/questions/29295838/org-apache-spark-sparkexception-task-not-serializable
          .map(toTuple)
          .filter(_.isDefined)
          .map(_.get)
          .foreach(println)
      )

    ssc.start
    ssc.awaitTerminationOrTimeout(sparkProperties.terminationTimeoutMillis)
  }
}

import scala.collection.JavaConverters._

object YelpHttpToStdoutService {
  def apply(sparkProperties: SparkProperties): YelpHttpToStdoutService = new YelpHttpToStdoutService(sparkProperties)

  val allowedFields = List("name", "rating", "review_count", "hours", "attributes")

  def toTuple(content: String) = {
    val objectMapper = new ObjectMapper

    Try(objectMapper.readValue(content, classOf[ObjectNode]))
      .filter(_.has("id"))
      .flatMap(node => {
        val id = node.get("id").textValue

        Try(objectMapper.writeValueAsString(node.retain(allowedFields.asJava)))
          .map(x => Some(Tuple2(id, x)))
      })
      .getOrElse(None)
  }
}

