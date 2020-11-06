@file:JvmName("rd")

package play.util

import java.util.concurrent.ThreadLocalRandom

/**
 * Random Utilities
 * @author LiangZengle
 */
@Suppress("NOTHING_TO_INLINE")
object rd {
  inline fun current() = ThreadLocalRandom.current()

  fun nextBoolean() = current().nextBoolean()

  fun nextInt() = current().nextInt()
  fun nextInt(bound: Int) = current().nextInt(bound)
  fun nextInt(origin: Int, bound: Int) = current().nextInt(origin, bound)
  fun nextIntClosed(origin: Int, bound: Int): Int {
    return if (bound == Int.MAX_VALUE) nextInt(origin, bound) else nextInt(origin, bound + 1)
  }

  fun nextLong() = current().nextLong()
  fun nextLong(bound: Long) = current().nextLong(bound)
  fun nextLong(origin: Long, bound: Long) = current().nextLong(origin, bound)
  fun nextLongClosed(origin: Long, bound: Long): Long {
    return if (bound == Long.MAX_VALUE) nextLong(origin, bound) else nextLong(origin, bound + 1)
  }

  fun nextFloat() = current().nextFloat()

  fun nextDouble() = current().nextDouble()

  fun test(prob: Int, boundProb: Int): Boolean = prob >= boundProb || prob > nextInt(boundProb)

  fun test(prob: Long, boundProb: Long): Boolean = prob >= boundProb || prob > nextLong(boundProb)

  fun testWith100(prob: Int): Boolean = test(prob, 100)
  fun testWith10000(prob: Int): Boolean = test(prob, 10000)

  fun test(prob: Float): Boolean {
    if (prob <= 0f) return false
    if (prob >= 1f) return true
    var f = prob
    var probInt = f.toInt()
    var bound = 1
    while (f != probInt.toFloat()) {
      f *= 10
      probInt = f.toInt()
      bound *= 10
    }
    return test(probInt, bound)
  }

  fun test(prob: Double): Boolean {
    if (prob <= 0.0) return false
    if (prob >= 1.0) return true
    var d = prob
    var probLong = d.toLong()
    var bound = 1L
    while (d != probLong.toDouble()) {
      d *= 10
      probLong = d.toLong()
      bound *= 10
    }
    return test(probLong, bound)
  }

  fun <T> random(list: List<T>): T {
    require(list.isNotEmpty()) { "list is empty." }
    return list[nextInt(list.size)]
  }

  fun <T> random(array: Array<T>): T {
    require(array.isNotEmpty()) { "array is empty." }
    return array[nextInt(array.size)]
  }

  fun <T> random(array: IntArray): Int {
    require(array.isNotEmpty()) { "array is empty." }
    return array[nextInt(array.size)]
  }

  fun <T> random(array: LongArray): Long {
    require(array.isNotEmpty()) { "array is empty." }
    return array[nextInt(array.size)]
  }

  fun <T> random(elems: Iterable<T>): T {
    if (elems is List<T>) return random(elems)
    val size = if (elems is Collection<T>) elems.size else {
      var count = 0
      for (e in elems) {
        count += 1
      }
      count
    }
    require(size > 0) { "elems is empty." }
    val idx = nextInt(size)
    var i = 0
    @Suppress("UseWithIndex")
    for (e in elems) {
      if (i == idx) {
        return e
      }
      i++
    }
    throw IllegalStateException("should not happen.")
  }

  fun <T> randomOne(elems: Iterable<T>, weigher: (T) -> Int): T {
    val totalProb = elems.sumBy(weigher)
    if (totalProb < 1) throw IllegalStateException("total prob is $totalProb")
    var r = nextInt(totalProb)
    for (elem in elems) {
      val weights = weigher(elem)
      if (weights > r) return elem
      r -= weights
    }
    throw IllegalStateException("should not happen.")
  }

  fun <T> random(elems: Iterable<T>, count: Int, weigher: (T) -> Int): List<T> {
    val totalProb = elems.sumBy(weigher)
    if (totalProb < 1) {
      return emptyList()
    }
    var n = 0
    val result = ArrayList<T>(count)
    while (n < count) {
      var r = nextInt(totalProb)
      for (elem in elems) {
        val weights = weigher(elem)
        if (weights > r) {
          n++
          result += elem
          break
        }
        r -= weights
      }
    }
    return result
  }

  fun <T> randomDistinct(iterable: Iterable<T>, expectedCount: Int, weigher: (T) -> Int): MutableList<T> {
    val elems = iterable.toMutableSet()
    val result = ArrayList<T>(expectedCount)
    while (result.size < expectedCount) {
      val totalProb = elems.sumBy(weigher)
      if (totalProb < 1) {
        break
      }
      var r = nextInt(totalProb)
      val it = elems.iterator()
      while (it.hasNext()) {
        val elem = it.next()
        val weights = weigher(elem)
        if (weights > r) {
          result += elem
          it.remove()
          break
        }
        r -= weights
      }
    }
    return result
  }
}

