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
class BoundedIntAdder {
  @JsonValue
  @Volatile
  private var value = 0

  companion object {
    private val valueUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedIntAdder::class.java, "value")

    @JvmStatic
    @JsonCreator
    private fun fromJson(node: IntNode): BoundedIntAdder {
      val adder = BoundedIntAdder()
      valueUpdater.set(adder, node.intValue())
      return adder
    }
  }

  /**
   * 增加
   *
   * @param delta 增加的值
   * @param bound 上限
   * @return 成功增加则返回增加后的值, 增加失败则返回当前的负数
   */
  fun add(delta: Int, bound: Int): Int {
    var oldValue: Int
    var newValue: Int
    do {
      oldValue = value
      if (bound <= oldValue) {
        return -oldValue
      }
      val longValue = oldValue.toLong() + delta
      if (longValue < 0 || longValue > bound) {
        return -oldValue
      }
      newValue = longValue.toInt()
    } while (!valueUpdater.compareAndSet(this, oldValue, newValue))
    return newValue
  }

  fun get(): Int = value
}
