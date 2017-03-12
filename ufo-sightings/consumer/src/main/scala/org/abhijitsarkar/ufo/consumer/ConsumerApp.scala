package org.abhijitsarkar.ufo.consumer

import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author Abhijit Sarkar
  */
object ConsumerApp extends Consumer {
  val config = ConfigFactory.load
  val consumerConfig = config.getConfig("sighting.consumer")
  val sparkConfig = consumerConfig.getConfig("spark")

  val conf = new SparkConf()
    .setMaster(sparkConfig.getString("master"))
    .setAppName("ufo-sighting")

  override implicit def sc: SparkContext = SparkContext.getOrCreate(conf)

  def main(args: Array[String]): Unit = {
    run(consumerConfig)
  }
}
