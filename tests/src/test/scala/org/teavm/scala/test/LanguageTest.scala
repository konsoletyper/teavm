package org.teavm.scala.test

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.teavm.junit.TeaVMTestRunner

@RunWith(classOf[TeaVMTestRunner])
class LanguageTest {
  @Test
  def lambda(): Unit = {
    assertEquals(6, Array(1, 2, 3).foldLeft(0)((a, b) => a + b))
  }
}
