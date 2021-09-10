package play.res

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavigableMapTest {

  class IntItem(val id: Int) : UniqueKey<Int> {
    override fun key(): Int = id

  }

  class LongItem(val id: Long) : UniqueKey<Long> {
    override fun key(): Long = id

  }

  @Test
  fun testInt() {
    val items = listOf(IntItem(1), IntItem(3), IntItem(5), IntItem(7), IntItem(9))
    val map = NavigableIntMap(items) { it.key() }

    assertEquals(map[1]!!.id, 1)
    assertEquals(map[5]!!.id, 5)

    assertEquals(map.lowerValue(0), null)
    assertEquals(map.lowerValue(1), null)
    assertEquals(map.lowerValue(2)!!.id, 1)
    assertEquals(map.lowerValue(5)!!.id, 3)
    assertEquals(map.lowerValue(6)!!.id, 5)
    assertEquals(map.lowerValue(7)!!.id, 5)
    assertEquals(map.lowerValue(8)!!.id, 7)
    assertEquals(map.lowerValue(9)!!.id, 7)
    assertEquals(map.lowerValue(10)!!.id, 9)

    assertEquals(map.higherValue(0)!!.id, 1)
    assertEquals(map.higherValue(1)!!.id, 3)
    assertEquals(map.higherValue(4)!!.id, 5)
    assertEquals(map.higherValue(5)!!.id, 7)
    assertEquals(map.higherValue(6)!!.id, 7)
    assertEquals(map.higherValue(7)!!.id, 9)
    assertEquals(map.higherValue(8)!!.id, 9)
    assertEquals(map.higherValue(9), null)

    assertEquals(map.lowerOrEqualValue(0), null)
    assertEquals(map.lowerOrEqualValue(1)!!.id, 1)
    assertEquals(map.lowerOrEqualValue(5)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(6)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(9)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10000)!!.id, 9)

    assertEquals(map.higherOrEqualValue(-9999)!!.id, 1)
    assertEquals(map.higherOrEqualValue(0)!!.id, 1)
    assertEquals(map.higherOrEqualValue(1)!!.id, 1)
    assertEquals(map.higherOrEqualValue(2)!!.id, 3)
    assertEquals(map.higherOrEqualValue(4)!!.id, 5)
    assertEquals(map.higherOrEqualValue(5)!!.id, 5)
    assertEquals(map.higherOrEqualValue(6)!!.id, 7)
    assertEquals(map.higherOrEqualValue(7)!!.id, 7)
    assertEquals(map.higherOrEqualValue(8)!!.id, 9)
    assertEquals(map.higherOrEqualValue(9)!!.id, 9)
    assertEquals(map.higherOrEqualValue(10), null)

    assertTrue(map.slice(1, true, 1, false).isEmpty())
    assertEquals(map.slice(1, true, 1, true).size, 1)
    assertEquals(map.slice(1, true, 1, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).size, 3)
    assertEquals(map.slice(1, true, 5, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).last().id, 5)
    assertEquals(map.slice(1, true, 9, true).last().id, 9)

    assertEquals(map.slice(1, false, 7, false).size, 2)
    assertEquals(map.slice(1, false, 5, false).first().id, 3)
    assertEquals(map.slice(1, false, 9, false).size, 3)
  }

  @Test
  fun testLont() {
    val items = listOf(LongItem(1), LongItem(3), LongItem(5), LongItem(7), LongItem(9))
    val map = NavigableLongMap(items) { it.key() }

    assertEquals(map[1]!!.id, 1)
    assertEquals(map[5]!!.id, 5)

    assertEquals(map.lowerValue(0), null)
    assertEquals(map.lowerValue(1), null)
    assertEquals(map.lowerValue(2)!!.id, 1)
    assertEquals(map.lowerValue(5)!!.id, 3)
    assertEquals(map.lowerValue(6)!!.id, 5)
    assertEquals(map.lowerValue(7)!!.id, 5)
    assertEquals(map.lowerValue(8)!!.id, 7)
    assertEquals(map.lowerValue(9)!!.id, 7)
    assertEquals(map.lowerValue(10)!!.id, 9)

    assertEquals(map.higherValue(0)!!.id, 1)
    assertEquals(map.higherValue(1)!!.id, 3)
    assertEquals(map.higherValue(4)!!.id, 5)
    assertEquals(map.higherValue(5)!!.id, 7)
    assertEquals(map.higherValue(6)!!.id, 7)
    assertEquals(map.higherValue(7)!!.id, 9)
    assertEquals(map.higherValue(8)!!.id, 9)
    assertEquals(map.higherValue(9), null)

    assertEquals(map.lowerOrEqualValue(0), null)
    assertEquals(map.lowerOrEqualValue(1)!!.id, 1)
    assertEquals(map.lowerOrEqualValue(5)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(6)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(9)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10000)!!.id, 9)

    assertEquals(map.higherOrEqualValue(-9999)!!.id, 1)
    assertEquals(map.higherOrEqualValue(0)!!.id, 1)
    assertEquals(map.higherOrEqualValue(1)!!.id, 1)
    assertEquals(map.higherOrEqualValue(2)!!.id, 3)
    assertEquals(map.higherOrEqualValue(4)!!.id, 5)
    assertEquals(map.higherOrEqualValue(5)!!.id, 5)
    assertEquals(map.higherOrEqualValue(6)!!.id, 7)
    assertEquals(map.higherOrEqualValue(7)!!.id, 7)
    assertEquals(map.higherOrEqualValue(8)!!.id, 9)
    assertEquals(map.higherOrEqualValue(9)!!.id, 9)
    assertEquals(map.higherOrEqualValue(10), null)

    assertTrue(map.slice(1, true, 1, false).isEmpty())
    assertEquals(map.slice(1, true, 1, true).size, 1)
    assertEquals(map.slice(1, true, 1, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).size, 3)
    assertEquals(map.slice(1, true, 5, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).last().id, 5)
    assertEquals(map.slice(1, true, 9, true).last().id, 9)

    assertEquals(map.slice(1, false, 7, false).size, 2)
    assertEquals(map.slice(1, false, 5, false).first().id, 3)
    assertEquals(map.slice(1, false, 9, false).size, 3)
  }

  @Test
  fun testObj() {
    val items = listOf(LongItem(1), LongItem(3), LongItem(5), LongItem(7), LongItem(9))
    val map = NavigableRefMap(items) { it.key() }

    assertEquals(map[1]!!.id, 1)
    assertEquals(map[5]!!.id, 5)

    assertEquals(map.lowerValue(0), null)
    assertEquals(map.lowerValue(1), null)
    assertEquals(map.lowerValue(2)!!.id, 1)
    assertEquals(map.lowerValue(5)!!.id, 3)
    assertEquals(map.lowerValue(6)!!.id, 5)
    assertEquals(map.lowerValue(7)!!.id, 5)
    assertEquals(map.lowerValue(8)!!.id, 7)
    assertEquals(map.lowerValue(9)!!.id, 7)
    assertEquals(map.lowerValue(10)!!.id, 9)

    assertEquals(map.higherValue(0)!!.id, 1)
    assertEquals(map.higherValue(1)!!.id, 3)
    assertEquals(map.higherValue(4)!!.id, 5)
    assertEquals(map.higherValue(5)!!.id, 7)
    assertEquals(map.higherValue(6)!!.id, 7)
    assertEquals(map.higherValue(7)!!.id, 9)
    assertEquals(map.higherValue(8)!!.id, 9)
    assertEquals(map.higherValue(9), null)

    assertEquals(map.lowerOrEqualValue(0), null)
    assertEquals(map.lowerOrEqualValue(1)!!.id, 1)
    assertEquals(map.lowerOrEqualValue(5)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(6)!!.id, 5)
    assertEquals(map.lowerOrEqualValue(9)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10)!!.id, 9)
    assertEquals(map.lowerOrEqualValue(10000)!!.id, 9)

    assertEquals(map.higherOrEqualValue(-9999)!!.id, 1)
    assertEquals(map.higherOrEqualValue(0)!!.id, 1)
    assertEquals(map.higherOrEqualValue(1)!!.id, 1)
    assertEquals(map.higherOrEqualValue(2)!!.id, 3)
    assertEquals(map.higherOrEqualValue(4)!!.id, 5)
    assertEquals(map.higherOrEqualValue(5)!!.id, 5)
    assertEquals(map.higherOrEqualValue(6)!!.id, 7)
    assertEquals(map.higherOrEqualValue(7)!!.id, 7)
    assertEquals(map.higherOrEqualValue(8)!!.id, 9)
    assertEquals(map.higherOrEqualValue(9)!!.id, 9)
    assertEquals(map.higherOrEqualValue(10), null)

    assertTrue(map.slice(1, true, 1, false).isEmpty())
    assertEquals(map.slice(1, true, 1, true).size, 1)
    assertEquals(map.slice(1, true, 1, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).size, 3)
    assertEquals(map.slice(1, true, 5, true).first().id, 1)
    assertEquals(map.slice(1, true, 5, true).last().id, 5)
    assertEquals(map.slice(1, true, 9, true).last().id, 9)

    assertEquals(map.slice(1, false, 7, false).size, 2)
    assertEquals(map.slice(1, false, 5, false).first().id, 3)
    assertEquals(map.slice(1, false, 9, false).size, 3)
  }
}
