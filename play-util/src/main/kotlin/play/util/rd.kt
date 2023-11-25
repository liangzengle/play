package play.util

import com.google.common.collect.Iterables
import play.util.primitive.safeMultiply
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max

/**
 * Random Utilities
 * @author LiangZengle
 */
@Suppress("ClassName", "unused")
object rd {

  @Suppress("NOTHING_TO_INLINE")
  private inline fun current() = ThreadLocalRandom.current()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.current()
   * ```
   */
  @JvmStatic
  fun random(): ThreadLocalRandom = current()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.current().nextBoolean()
   * ```
   */
  @JvmStatic
  fun nextBoolean() = current().nextBoolean()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextInt()
   * ```
   */
  @JvmStatic
  fun nextInt() = current().nextInt()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextInt(boundExclusive)
   * ```
   * @return random int in [0, [boundExclusive])
   */
  @JvmStatic
  fun nextInt(boundExclusive: Int) = current().nextInt(boundExclusive)

  /**
   * @return random int in [[origin], [boundExclusive])
   */
  @JvmStatic
  fun nextInt(origin: Int, boundExclusive: Int) = current().nextInt(origin, boundExclusive)

  /**
   * @return random int in [[origin], [boundInclusive]]
   */
  @JvmStatic
  fun nextIntClosed(origin: Int, boundInclusive: Int): Int {
    return if (origin != Int.MIN_VALUE && boundInclusive != Int.MAX_VALUE)
      nextInt(origin, boundInclusive + 1)
    else
      nextLongClosed(origin.toLong(), boundInclusive.toLong()).toInt()
  }

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextLong()
   * ```
   */
  @JvmStatic
  fun nextLong() = current().nextLong()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextInt(boundExclusive)
   * ```
   * @return random long in [0, [boundExclusive])
   */
  @JvmStatic
  fun nextLong(boundExclusive: Long) = current().nextLong(boundExclusive)

  /**
   * @return random long in [[origin], [boundExclusive])
   */
  @JvmStatic
  fun nextLong(origin: Long, boundExclusive: Long) = current().nextLong(origin, boundExclusive)

  /**
   * @return random long in [[origin], [boundInclusive]]
   */
  @JvmStatic
  fun nextLongClosed(origin: Long, boundInclusive: Long): Long {
    return if (boundInclusive != Long.MAX_VALUE) {
      nextLong(origin, boundInclusive + 1)
    } else {
      if (origin == Long.MIN_VALUE) {
        val v = nextLong(origin, boundInclusive)
        if (v == Long.MIN_VALUE && nextBoolean()) Long.MIN_VALUE else Long.MAX_VALUE
      } else {
        nextLong(origin - 1, boundInclusive) + 1
      }
    }
  }

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextFloat()
   * ```
   */
  @JvmStatic
  fun nextFloat() = current().nextFloat()

  /**
   * equals:
   * ```
   * ThreadLocalRandom.nextDouble()
   * ```
   */
  @JvmStatic
  fun nextDouble() = current().nextDouble()

  /**
   * equals:
   * ```
   * Collections.shuffle(list, ThreadLocalRandom.current())
   * ```
   */
  @JvmStatic
  fun <T> shuffle(elements: MutableList<T>) {
    elements.shuffle(current())
  }

  /**
   * Test if ``` prob > nextInt(boundProb) ```
   */
  @JvmStatic
  fun test(prob: Int, boundProb: Int): Boolean = prob > 0 && (prob >= boundProb || prob > nextInt(boundProb))

  /**
   * Test if ``` prob > nextInt(boundProb) ```
   */
  @JvmStatic
  fun test(prob: Long, boundProb: Long): Boolean = prob > 0 && (prob >= boundProb || prob > nextLong(boundProb))

  /**
   * Test if ``` prob > nextInt(100) ```
   */
  @JvmStatic
  fun testWith100(prob: Int): Boolean = test(prob, 100)

  /**
   * Test if ``` prob > nextInt(10000) ```
   */
  @JvmStatic
  fun testWith10000(prob: Int): Boolean = test(prob, 10000)

  /**
   * Test if ``` prob > nextFloat() ```
   */
  @JvmStatic
  fun test(prob: Float): Boolean {
    if (prob <= 0f) return false
    if (prob >= 1f) return true
    var f = prob
    var probInt = f.toInt()
    var bound = 1
    while (f != probInt.toFloat()) {
      f *= 10
      probInt = f.toInt()
      bound = bound safeMultiply 10
    }
    return test(probInt, bound)
  }

  /**
   * Test if ``` prob > nextDouble() ```
   */
  @JvmStatic
  fun test(prob: Double): Boolean {
    if (prob <= 0.0) return false
    if (prob >= 1.0) return true
    var d = prob
    var probLong = d.toLong()
    var bound = 1L
    while (d != probLong.toDouble()) {
      d *= 10
      probLong = d.toLong()
      bound = bound safeMultiply 10
    }
    return test(probLong, bound)
  }

  /**
   * 从[list]中随机一个元素
   *
   * @return if [list] is null or empty, returns null
   */
  @JvmStatic
  fun <T> randomOrNull(list: List<T>?): T? {
    if (list.isNullOrEmpty()) return null
    return list[nextInt(list.size)]
  }

  /**
   * 从[elems]中随机一个元素
   *
   * @return if [elems] is null or empty, returns null
   */
  @JvmStatic
  fun <T> randomOrNull(elems: Iterable<T>?): T? {
    if (elems == null) return null
    if (elems is List<T>) return random(elems)
    val size = elems.count()
    if (size == 0) {
      return null
    }
    val idx = nextInt(size)
    return Iterables.get(elems, idx)
  }

  /**
   * 从[nonEmptyList]中随机一个元素
   *
   * @return if [nonEmptyList] is empty, [IllegalArgumentException] will throw
   */
  @JvmStatic
  fun <T> random(nonEmptyList: List<T>): T {
    require(nonEmptyList.isNotEmpty()) { "list is empty." }
    return nonEmptyList[nextInt(nonEmptyList.size)]
  }

  /**
   * 从[nonEmptyArray]中随机一个元素
   *
   * @return if [nonEmptyArray] is empty, [IllegalArgumentException] will throw
   */
  @JvmStatic
  fun <T> random(nonEmptyArray: Array<T>): T {
    require(nonEmptyArray.isNotEmpty()) { "array is empty." }
    return nonEmptyArray[nextInt(nonEmptyArray.size)]
  }

  /**
   * 从[nonEmptyArray]中随机一个元素
   *
   * @return if [nonEmptyArray] is empty, [IllegalArgumentException] will throw
   */
  @JvmStatic
  fun <T> random(nonEmptyArray: IntArray): Int {
    require(nonEmptyArray.isNotEmpty()) { "array is empty." }
    return nonEmptyArray[nextInt(nonEmptyArray.size)]
  }

  /**
   * 从[nonEmptyArray]中随机一个元素
   *
   * @return if [nonEmptyArray] is empty, [IllegalArgumentException] will throw
   */
  @JvmStatic
  fun <T> random(nonEmptyArray: LongArray): Long {
    require(nonEmptyArray.isNotEmpty()) { "array is empty." }
    return nonEmptyArray[nextInt(nonEmptyArray.size)]
  }

  /**
   * 从[nonEmptyElems]中随机一个元素
   *
   * @return if [nonEmptyElems] is empty, [IllegalArgumentException] will throw
   */
  @JvmStatic
  fun <T> random(nonEmptyElems: Iterable<T>): T {
    if (nonEmptyElems is List<T>) return random(nonEmptyElems)
    val size = nonEmptyElems.count()
    require(size > 0) { "elems is empty." }
    val idx = nextInt(size)
    return Iterables.get(nonEmptyElems, idx)
  }

  /**
   * 根据权重随机一个元素
   *
   * @param elems 元素列表
   * @param weigher 权重计算器
   * @return 随机一个元素
   */
  @JvmStatic
  fun <T> random(elems: Iterable<T>, weigher: (T) -> Int): T {
    val totalProb = elems.sumOf { max(weigher(it), 0) }
    if (totalProb < 1) throw IllegalStateException("total prob is $totalProb")
    var r = nextInt(totalProb)
    for (elem in elems) {
      val weights = weigher(elem)
      if (weights < 1) {
        continue
      }
      if (weights > r) return elem
      r -= weights
    }
    throw IllegalStateException("should not happen.")
  }

  /**
   * 根据权重随机[count]个元素
   *
   * @param elems 元素列表
   * @param count 随机数量
   * @param weigher 权重计算器
   * @return 随机的[count]个元素
   */
  @JvmStatic
  fun <T> random(elems: Iterable<T>, count: Int, weigher: (T) -> Int): List<T> {
    return random(elems, count, weigher, ArrayList(count)) { list, elem -> list.apply { add(elem) } }
  }

  @JvmStatic
  fun <T, R, R1 : R> random(
    elems: Iterable<T>,
    count: Int,
    weigher: (T) -> Int,
    initial: R1,
    accumulator: (R1, T) -> R1
  ): R {
    val totalProb = elems.sumOf { max(weigher(it), 0) }
    if (totalProb < 1) {
      return initial
    }
    var n = 0
    var result = initial
    while (n < count) {
      var r = nextInt(totalProb)
      for (elem in elems) {
        val weights = weigher(elem)
        if (weights < 1) {
          continue
        }
        if (weights > r) {
          n++
          result = accumulator(result, elem)
          break
        }
        r -= weights
      }
    }
    return result
  }

  /**
   * 去重随机[expectedCount]个元素
   *
   * @param distinctElements 元素列表
   * @param expectedCount 随机数量
   * @param weigher 权重计算器
   * @return 随机的元素列表，如果[distinctElements]的个数小于[expectedCount]，则返回的元素数量等于[distinctElements]的个数
   */
  @JvmStatic
  fun <T> randomDistinct(distinctElements: Collection<T>, expectedCount: Int, weigher: (T) -> Int): MutableList<T> {
    if (distinctElements.size < expectedCount) {
      return distinctElements.toMutableList()
    }
    val candidates = distinctElements.toCollection(LinkedList())
    val result = ArrayList<T>(expectedCount)
    var totalProb = candidates.sumOf { max(weigher(it), 0) }
    while (result.size < expectedCount) {
      if (totalProb < 1) {
        break
      }
      var r = nextInt(totalProb)
      val it = candidates.iterator()
      while (it.hasNext()) {
        val elem = it.next()
        val weights = weigher(elem)
        if (weights < 1) {
          continue
        }
        if (weights > r) {
          result += elem
          it.remove()
          totalProb -= weights
          break
        }
        r -= weights
      }
    }
    return result
  }
}
