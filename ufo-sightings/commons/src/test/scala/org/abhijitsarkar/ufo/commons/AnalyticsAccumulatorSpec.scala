package org.abhijitsarkar.ufo.commons

import org.scalatest.{FlatSpec, Matchers}

/**
  * @author Abhijit Sarkar
  */

class AnalyticsAccumulatorSpec extends FlatSpec with Matchers {
  "AnalyticsAccumulator" should "add default value" in {
    val analytics = new AnalyticsAccumulator
    analytics.add(("k1", "k2"))
    analytics.add(("k1", "k2"))
    analytics.add(("k1", "k3"))
    analytics.add(("k4", "k5"))

    analytics.value("k1") should contain(("k2", 2))
    analytics.value("k1") should contain(("k3", 1))
    analytics.value("k4") should contain(("k5", 1))
  }

  it should "add given value" in {
    val analytics = new AnalyticsAccumulator
    analytics.add(("k1", "k2"), 3)
    analytics.add(("k1", "k2"))
    analytics.add(("k1", "k3"), 2)
    analytics.add(("k4", "k5"))

    analytics.value("k1") should contain(("k2", 4))
    analytics.value("k1") should contain(("k3", 2))
    analytics.value("k4") should contain(("k5", 1))
  }

  it should "return a new object when copied" in {
    val analytics = new AnalyticsAccumulator
    val copy = analytics.copy

    analytics.equals(copy) shouldBe (false)
  }

  it should "delete all data when cleared" in {
    val analytics = new AnalyticsAccumulator
    analytics.add(("k1", "k2"))

    analytics.clear()

    analytics.isEmpty shouldBe (true)
  }
}
