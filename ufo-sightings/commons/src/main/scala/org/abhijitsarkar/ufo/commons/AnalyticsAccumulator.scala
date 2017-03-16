package org.abhijitsarkar.ufo.commons

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{BiConsumer, BiFunction}
import java.util.{Map => JavaMap}

/**
  * @author Abhijit Sarkar
  */
// Needs to be serializable since it's called from Spark foreach
// c.f. http://stackoverflow.com/questions/22592811/task-not-serializable-java-io-notserializableexception-when-calling-function-ou
class AnalyticsAccumulator extends Serializable {
  type Accumulator = JavaMap[String, Int]
  private val _value: JavaMap[String, JavaMap[String, Int]] = new ConcurrentHashMap[String, Accumulator]

  import scala.collection.JavaConverters._

  def value: Analytics = _value.asScala.toMap.mapValues(_.asScala.toMap)

  def isEmpty = _value.isEmpty

  def clear() = _value.clear

  def copy = {
    val copy = new AnalyticsAccumulator

    _value.asScala
      .foreach(x => merge(x._1, x._2, copy._value))

    copy
  }

  def add(k: (String, String), v: Int = 1): Unit = merge(k._1, singletonMap(k._2, v), _value)

  private[this] def singletonMap(k: String, v: Int = 1) = {
    val m = new ConcurrentHashMap[String, Int]
    m.put(k, v)

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
