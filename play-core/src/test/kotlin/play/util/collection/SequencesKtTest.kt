package play.util.collection

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

/**
 *
 * @author LiangZengle
 */
class SequencesKtTest {

  @Test
  fun sizeCompareTo() {
    val s = listOf(1, 2, 3).asSequence()
    assertEquals(s.sizeCompareTo(0), 1)
    assertEquals(s.sizeCompareTo(2), 1)
    assertEquals(s.sizeCompareTo(-1), 1)
    assertEquals(s.sizeCompareTo(3), 0)
    assertEquals(s.sizeCompareTo(4), -1)
  }

  @Test
  fun sizeE() {
    val s = listOf(1, 2, 3).asSequence()
    assertTrue(s.sizeEq(3))
    assertFalse(s.sizeEq(1))
    assertFalse(s.sizeEq(-1))
    assertFalse(s.sizeEq(4))
  }

  @Test
  fun sizeGT() {
    val s = listOf(1, 2, 3).asSequence()
    assertFalse(s.sizeGt(3))
    assertFalse(s.sizeGt(4))
    assertTrue(s.sizeGt(1))
    assertTrue(s.sizeGt(-1))
    assertTrue(s.sizeGt(2))
  }

  @Test
  fun sizeGE() {
    val s = listOf(1, 2, 3).asSequence()
    assertTrue(s.sizeGe(3))
    assertFalse(s.sizeGe(4))
    assertTrue(s.sizeGe(1))
    assertTrue(s.sizeGe(-1))
    assertTrue(s.sizeGe(2))
  }

  @Test
  fun sizeLT() {
    val s = listOf(1, 2, 3).asSequence()
    assertFalse(s.sizeLt(3))
    assertTrue(s.sizeLt(4))
    assertFalse(s.sizeLt(1))
    assertFalse(s.sizeLt(-1))
    assertFalse(s.sizeLt(2))
  }

  @Test
  fun sizeLE() {
    val s = listOf(1, 2, 3).asSequence()
    assertTrue(s.sizeLe(3))
    assertTrue(s.sizeLe(4))
    assertFalse(s.sizeLt(1))
    assertFalse(s.sizeLt(-1))
    assertFalse(s.sizeLt(2))
  }
}
