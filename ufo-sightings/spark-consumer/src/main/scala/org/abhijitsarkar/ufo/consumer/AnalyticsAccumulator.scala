package org.abhijitsarkar.ufo.consumer

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{BiConsumer, BiFunction}
import java.util.{Map => JavaMap}

import org.apache.spark.util.AccumulatorV2

/**
  * @author Abhijit Sarkar
  */
class AnalyticsAccumulator extends AccumulatorV2[(String, String), JavaMap[String, JavaMap[String, Int]]] {
  type Accumulator = JavaMap[String, Int]

  val analytics = new ConcurrentHashMap[String, Accumulator]

  override def isZero: Boolean = analytics.isEmpty

  override def copy(): AccumulatorV2[(String, String), JavaMap[String, Accumulator]] = {
    val copy = new AnalyticsAccumulator

    import scala.collection.JavaConverters._
    analytics.asScala
      .foreach(x => merge(x._1, x._2, copy.analytics))

    copy
  }

  override def reset(): Unit = analytics.clear

  override def add(v: (String, String)): Unit = merge(v._1, singletonMap(v._2), analytics)

  override def merge(other: AccumulatorV2[(String, String), JavaMap[String, Accumulator]]): Unit = {
    import scala.collection.JavaConverters._

    other.value.asScala
      .foreach(x => merge(x._1, x._2, analytics))
  }

  override def value: JavaMap[String, Accumulator] = analytics

  private[this] def singletonMap(k: String) = {
    val m = new ConcurrentHashMap[String, Int]
    m.put(k, 1)

    m
  }

  private[this] def merge(k: String, acc: Accumulator, map: JavaMap[String, Accumulator]) = {
    map.merge(k, acc,
      new BiFunction[Accumulator, Accumulator, Accumulator] {
        override def apply(oldMap: Accumulator, newMap: Accumulator): Accumulator = {
          newMap.forEach(new BiConsumer[String, Int] {
            override def accept(k: String, v: Int): Unit = {
              oldMap.merge(k, v, new BiFunction[Int, Int, Int] {
                override def apply(i: Int, j: Int): Int = i + j
              })
            }
          })
          oldMap
        }
      })
  }
}
