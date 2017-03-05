package org.abhijitsarkar

import com.couchbase.client.java.document.RawJsonDocument
import com.couchbase.spark.StoreMode.UPSERT
import com.typesafe.config.ConfigFactory
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

/**
  * @author Abhijit Sarkar
  */
object YelpDataloadApp extends App {
  if (args.isEmpty)
    throw new IllegalArgumentException("Usage: YelpDataloadApp <url> [couchbase]")

  val conf = ConfigFactory.load()
  val sparkConf = conf.getConfig("spark")

  val scBuilder = SparkSession.builder()
    .master(sparkConf.getString("master"))
    .appName("yelp-dataload")

  val (sc, sink) = args.lift(1) match {
    case Some("couchbase") => {
      import com.couchbase.spark._
      val couchbaseConf = conf.getConfig("couchbase")
      val bucketName = couchbaseConf.getString("bucketName")

      val couchbaseSink = (rdd: RDD[(String, String)]) =>
        rdd.map(t => RawJsonDocument.create(t._1, t._2))
          .saveToCouchbase(bucketName, UPSERT)
      val sc = scBuilder
        .config("spark.couchbase.nodes", couchbaseConf.getString("nodes"))
        .config(s"spark.couchbase.bucket.$bucketName",
          couchbaseConf.getString("bucketPassword"))
        .getOrCreate
        .sparkContext

      (sc, couchbaseSink)
    }
    case _ => {
      val consoleSink = (rdd: RDD[(String, String)]) => rdd.foreach(println)
      val sc = scBuilder
        .getOrCreate
        .sparkContext

      (sc, consoleSink)
    }
  }

  val url = args(0)
  YelpDataloadService(sparkConf.getLong("terminationTimeoutMillis"), sc, sink)
    .load(url)
}
