package org.abhijitsarkar

import com.typesafe.config.ConfigFactory

/**
  * @author Abhijit Sarkar
  */
object YelpDataloadApp extends App {
  if (args.isEmpty)
    throw new IllegalArgumentException("Usage: YelpDataloadApp <url> [couchbase]")

  val (sc, sink) = JobContextFactory()(args.lift(1))

  val url = args(0)
  val conf = ConfigFactory.load
  val sparkConf = conf.getConfig("spark")
  YelpDataloadService(sparkConf.getLong("terminationTimeoutMillis"), sc, sink)
    .load(url)
}
