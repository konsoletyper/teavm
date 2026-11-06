package org.teavm.scala.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.teavm.junit.TeaVMTest

@TeaVMTest
class LanguageTest {
  @Test
  def lambda(): Unit = {
    assertEquals(6, Array(1, 2, 3).foldLeft(0)((a, b) => a + b))
  }
}
