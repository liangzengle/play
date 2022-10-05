package play.util.primitive

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 *
 * @author LiangZengle
 */
internal class BitTest {

  @Test
  fun set() {
    assertEquals(0b1, Bit.set(0, 1))
    assertEquals(0b10, Bit.set(0, 2))
    assertEquals(0b100, Bit.set(0, 3))
    assertEquals(0b1000, Bit.set(0, 4))
    assertEquals(0b10000, Bit.set(0, 5))
    assertEquals(0b100000, Bit.set(0, 6))
    assertEquals(-0b1000_0000_0000_0000_0000_0000_0000_0000, Bit.set(0, 32))
  }

  @Test
  fun clear() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set(value, 10)
    value = Bit.set(value, 10)
    assertEquals(0, Bit.clear(value, 10))
  }

  @Test
  fun is1() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set(value, 10)
    assertTrue(Bit.is1(value, 10))
    assertFalse(Bit.is1(value, 9))
  }

  @Test
  fun is0() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set(value, 10)
    assertTrue(Bit.is0(value, 9))
  }

  @Test
  fun testSet() {
    assertEquals(0b1, Bit.set(0L, 1))
    assertEquals(
      0b0000_0000_0000_0000_0000_0000_0000_0000_1000_0000_0000_0000_0000_0000_0000_0000L,
      Bit.set(0L, 32)
    )
    assertEquals(Long.MIN_VALUE, Bit.set(0L, 64))
    assertEquals(
      0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L,
      Bit.set(0L, 63)
    )
  }

  @Test
  fun testClear() {
    val value = 0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L
    assertEquals(0, Bit.clear(value, 63))
  }

  @Test
  fun testIs1() {
    val value = 0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L
    assertTrue(Bit.is1(value, 63))
  }

  @Test
  fun testIs0() {
    val value = 0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L
    assertTrue(Bit.is0(value, 64))
  }
}
