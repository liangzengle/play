package play.util.concurrent

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.IntNode
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import javax.annotation.concurrent.ThreadSafe

/**
 * 有界的非负数int加法器
 * @author LiangZengle
 */
@ThreadSafe
class BoundedIntAdder(initialValue: Int) {
  init {
    require(initialValue >= 0) { "Illegal initialValue:$initialValue" }
  }

  @JsonValue
  @Volatile
  private var value = initialValue

  constructor() : this(0)

  companion object {
    private val valueUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedIntAdder::class.java, "value")

    @JvmStatic
    @JsonCreator
    private fun fromJson(node: IntNode): BoundedIntAdder {
      return BoundedIntAdder(node.intValue())
    }
  }

  /**
   * 增加
   *
   * @param delta 增加的值，可以是负数
   * @param higherBound 上限
   * @return 变化值
   */
  fun add(delta: Int, higherBound: Int): Int {
    var oldValue: Int
    var newValue: Int
    do {
      oldValue = value
      if (higherBound <= oldValue) {
        return 0
      }

      val longValue = (oldValue.toLong() + delta).coerceAtLeast(0).coerceAtMost(higherBound.toLong())
      newValue = longValue.toInt()
    } while (!valueUpdater.compareAndSet(this, oldValue, newValue))
    return newValue - oldValue
  }

  fun decrease(): Boolean {
    var oldValue: Int
    var newValue: Int
    do {
      oldValue = value
      if (oldValue <= 0) {
        return false
      }
      newValue = oldValue - 1
    } while (!valueUpdater.compareAndSet(this, oldValue, newValue))
    return true
  }

  fun get(): Int = value
}
