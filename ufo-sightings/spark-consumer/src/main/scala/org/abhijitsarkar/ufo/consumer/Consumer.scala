package org.abhijitsarkar.ufo.consumer

import com.typesafe.config.Config
import org.abhijitsarkar.ufo.domain.Sighting
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkContext
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

import scala.util.Try

/**
  * @author Abhijit Sarkar
  */
trait Consumer {
  implicit def sc: SparkContext

  import scala.collection.JavaConverters._

  def run(consumerConfig: Config) = {
    val kafkaConfig = consumerConfig.getConfig("kafka")

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kafkaConfig.getStringList("bootstrap.servers"),
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "spark-consumer",
      "auto.offset.reset" -> kafkaConfig.getString("auto.offset.reset"),
      "enable.auto.commit" -> (kafkaConfig.getBoolean("enable.auto.commit"): java.lang.Boolean)
    )
      .asJava

    val sparkConfig = consumerConfig.getConfig("spark")
    val ssc = new StreamingContext(sc, Milliseconds(Try(sparkConfig.getLong("batchDurationMillis"))
      .getOrElse(1000L)))
    val topics = Seq("ufo").asJava
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
      // Kafka ConsumerRecord is not serializable. Use .map to extract fields before calling .persist or .window
      // org.apache.spark.SparkException: Task not serializable
      .map(record => (record.key, record.value))
      .persist

    import org.abhijitsarkar.ufo.domain.SightingProtocol._
    import spray.json._
    val byState = stream
      .map(_._2.parseJson.convertTo[Sighting])
      .filter(_.state.isDefined)
      .map(_.state.get)
      .countByValue()

    println("--- New stream by state ---")
    byState
      .foreachRDD(rdd => {
        println("--- New RDD by state ---")
        rdd.foreach(println)
      })

    ssc.start
    ssc.awaitTerminationOrTimeout(Try(sparkConfig.getLong("terminationTimeoutMillis"))
      .getOrElse(60000L))
  }
}
