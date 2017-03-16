package org.abhijitsarkar.ufo.consumer

import com.typesafe.config.Config
import org.abhijitsarkar.ufo.commons.Sighting
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

  def run(consumerConfig: Config, analytics: AnalyticsAccumulatorV2) = {
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
    val ssc = new StreamingContext(sc, Milliseconds(Try(sparkConfig.getLong("batchIntervalMillis"))
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

    import org.abhijitsarkar.ufo.commons.SightingProtocol._
    import spray.json._

    type MaybeValue = (String, Option[String])
    stream
      .map(_._2.parseJson.convertTo[Sighting])
      .map(x =>
        (("state", x.state),
          ("shape", x.shape),
          ("month", x.eventDateTime.map(_.getMonth.name)),
          ("year", x.eventDateTime.map(_.getYear.toString))))
      .foreachRDD(_.foreach(_.productIterator
        .map(_.asInstanceOf[MaybeValue]) // Tuples can be heterogeneous, thus productIterator returns Any
        .foreach(x => x._2.foreach(y => analytics.add((x._1, y))))))

    ssc.start
    ssc.awaitTerminationOrTimeout(Try(sparkConfig.getLong("terminationTimeoutMillis"))
      .getOrElse(60000L))
  }
}
