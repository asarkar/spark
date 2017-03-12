package org.abhijitsarkar.ufo.consumer

import com.typesafe.config.Config
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkContext
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

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
      "group.id" -> "ufo",
      "auto.offset.reset" -> kafkaConfig.getString("auto.offset.reset"),
      "enable.auto.commit" -> (kafkaConfig.getBoolean("enable.auto.commit"): java.lang.Boolean)
    )
    val sparkConfig = consumerConfig.getConfig("spark")
    val ssc = new StreamingContext(sc, Milliseconds(sparkConfig.getLong("batchDurationMillis")))
    val topics = Seq(kafkaConfig.getString("topic")).asJava
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams.asJava)
    )

    stream
      .map(record => (record.key, record.value))
      .foreachRDD(_.foreach(println))

    ssc.start
    ssc.awaitTerminationOrTimeout(sparkConfig.getLong("terminationTimeoutMillis"))
  }
}
