package org.teavm.scala

import org.junit.Test
import org.junit.runner.RunWith
import org.teavm.junit.TeaVMTestRunner
import org.junit.Assert.assertEquals

@RunWith(classOf[TeaVMTestRunner])
class LazyValueTest {
  private var value: Int = 0

  private lazy val lazyValue: Int = {
    value += 1
    23
  }

  @Test
  def lazyValueSupport(): Unit = {
    assertEquals(0, value)
    assertEquals(23, lazyValue)
    assertEquals(1, value)
  }
}