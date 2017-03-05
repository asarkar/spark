package org.abhijitsarkar

import com.couchbase.client.java.document.RawJsonDocument
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * @author Abhijit Sarkar
  */

import org.abhijitsarkar.JobContextFactory._

class JobContextFactory extends PartialFunction[Option[String], (SparkContext, Sink)] {
  val log = Logger.getLogger(classOf[JobContextFactory])

  // Caches
  val conf = ConfigFactory.load()
  val sparkConf = conf.getConfig("spark")

  val scBuilder = SparkSession.builder()
    .master(sparkConf.getString("master"))
    .appName("yelp-dataload")

  override def isDefinedAt(x: Option[String]): Boolean = x.isDefined

  override def apply(destination: Option[String]): (SparkContext, Sink) = destination match {
    case Some("couchbase") => {
      log.info("Destination Couchbase.")

      import com.couchbase.spark._
      val couchbaseConf = conf.getConfig("couchbase")
      val bucketName = couchbaseConf.getString("bucketName")

      val couchbaseSink = (rdd: RDD[(String, String)]) =>
        rdd.map(t => RawJsonDocument.create(t._1, t._2))
          .saveToCouchbase()
      val sc = scBuilder
        .config("spark.couchbase.nodes", couchbaseConf.getString("nodes"))
        .config(s"spark.couchbase.bucket.$bucketName",
          couchbaseConf.getString("bucketPassword"))
        .getOrCreate
        .sparkContext

      (sc, couchbaseSink)
    }
    case _ => {
      log.info("Destination console.")

      val consoleSink = (rdd: RDD[(String, String)]) => rdd.foreach(println)
      val sc = scBuilder
        .getOrCreate
        .sparkContext

      (sc, consoleSink)
    }
  }
}

object JobContextFactory {
  type Sink = RDD[(String, String)] => Unit

  def apply(): JobContextFactory = new JobContextFactory
}
