package org.abhijitsarkar.ufo.commons

import scala.collection.immutable.TreeMap

/**
  * @author Abhijit Sarkar
  */
object PrettyPrinter {
  private[this] val printFormat: String = "| %-12s | %-8s |%n"

  def print(analytics: Map[String, Map[String, Int]]) {
    List("state", "shape", "month", "year")
      .foreach(key => {
        System.out.format("+--------------+----------+%n")
        System.out.format(printFormat, key.toUpperCase, "COUNT")
        System.out.format("+--------------+----------+%n")
        analytics.get(key)
          .foreach(x => TreeMap(x.toSeq: _*)
            .foreach(t => System.out.format(printFormat, t._1, t._2.toString)))
        System.out.format("+--------------+----------+%n")
      })
  }
}
