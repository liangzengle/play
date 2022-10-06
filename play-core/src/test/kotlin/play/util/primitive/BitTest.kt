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
    assertEquals(0b1, Bit.set1(0, 1))
    assertEquals(0b10, Bit.set1(0, 2))
    assertEquals(0b100, Bit.set1(0, 3))
    assertEquals(0b1000, Bit.set1(0, 4))
    assertEquals(0b10000, Bit.set1(0, 5))
    assertEquals(0b100000, Bit.set1(0, 6))
    assertEquals(-0b1000_0000_0000_0000_0000_0000_0000_0000, Bit.set1(0, 32))
  }

  @Test
  fun clear() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set1(value, 10)
    value = Bit.set1(value, 10)
    assertEquals(0, Bit.set0(value, 10))
  }

  @Test
  fun is1() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set1(value, 10)
    assertTrue(Bit.is1(value, 10))
    assertFalse(Bit.is1(value, 9))
  }

  @Test
  fun is0() {
    var value = 0x0000_0000_0000_0000_0000_0000_0000_0000
    value = Bit.set1(value, 10)
    assertTrue(Bit.is0(value, 9))
  }

  @Test
  fun testSet() {
    assertEquals(0b1, Bit.set1(0L, 1))
    assertEquals(
      0b0000_0000_0000_0000_0000_0000_0000_0000_1000_0000_0000_0000_0000_0000_0000_0000L,
      Bit.set1(0L, 32)
    )
    assertEquals(Long.MIN_VALUE, Bit.set1(0L, 64))
    assertEquals(
      0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L,
      Bit.set1(0L, 63)
    )
  }

  @Test
  fun testClear() {
    val value = 0b0100_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000L
    assertEquals(0, Bit.set0(value, 63))
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

  @Test
  fun testIntArraySet1() {
    var array = intArrayOf(0, 0)
    Bit.set1(array, 33)
    Bit.set1(array, 64)

    assertEquals(2, array.size)
    assertTrue(Bit.is1(array, 33))
    assertTrue(Bit.is1(array, 64))

    array = Bit.set1(array, 67)
    assertEquals(3, array.size)
    assertTrue(Bit.is1(array, 67))
  }

  @Test
  fun testLongArraySet1() {
    var array = longArrayOf(0)

    Bit.set1(array, 33)
    Bit.set1(array, 64)
    assertEquals(1, array.size)
    assertTrue(Bit.is1(array, 33))
    assertTrue(Bit.is1(array, 64))

    array = Bit.set1(array, 67)
    assertEquals(2, array.size)
    assertTrue(Bit.is1(array, 67))

    array = Bit.set1(array, 188)
    assertEquals(3, array.size)
    assertTrue(Bit.is1(array, 188))

    array = Bit.set1(array, 192)
    assertEquals(3, array.size)
    assertTrue(Bit.is1(array, 192))
  }
}
