package org.abhijitsarkar.ufo.consumer

import org.abhijitsarkar.ufo.commons.{Analytics, AnalyticsAccumulator}
import org.apache.spark.util.AccumulatorV2

/**
  * @author Abhijit Sarkar
  */
class AnalyticsAccumulatorV2(private val analytics: AnalyticsAccumulator = new AnalyticsAccumulator)
  extends AccumulatorV2[(String, String), Analytics] {
  override def isZero: Boolean = analytics.isEmpty

  override def copy(): AccumulatorV2[(String, String), Analytics] = new AnalyticsAccumulatorV2(analytics.copy)

  override def reset(): Unit = analytics.clear()

  override def add(v: (String, String)): Unit = analytics.add(v)

  override def merge(other: AccumulatorV2[(String, String), Analytics]): Unit = other.value
    .flatMap(x => x._2.toSeq.map(y => (x._1, y._1, y._2)))
    .foreach(x => analytics.add((x._1, x._2), x._3))

  override def value: Analytics = analytics.value
}
