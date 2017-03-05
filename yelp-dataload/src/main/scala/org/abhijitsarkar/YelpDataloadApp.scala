package org.abhijitsarkar

/**
  * @author Abhijit Sarkar
  */
object YelpDataloadApp extends App {
  if (args.isEmpty)
    throw new IllegalArgumentException("Usage: YelpDataloadApp <url>")

  val sparkProperties = SparkProperties("local[*]", 60000L)

  YelpHttpToStdoutService(sparkProperties)
    .load(args(0))
}
