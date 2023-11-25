package play.res.support

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @author LiangZengle
 */
enum class CompareOperator {
  EQ {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult == 0
    }
  },
  NE {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult != 0
    }
  },
  GT {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult > 0
    }
  },
  LT {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult < 0
    }
  },
  GE {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult >= 0
    }
  },
  LE {
    override fun checkResult(compareResult: Int): Boolean {
      return compareResult <= 0
    }
  };

  /**
   * 检测输入值是否满足期望值
   *
   * @param expected 期望值
   * @param given 输入值
   * @return 是否满足
   */
  fun check(expected: Int, given: Int): Boolean {
    return checkResult(given.compareTo(expected))
  }

  /**
   * 检测输入值是否满足期望值
   *
   * @param expected 期望值
   * @param given 输入值
   * @return 是否满足
   */
  fun check(expected: Long, given: Long): Boolean {
    return checkResult(given.compareTo(expected))
  }

  /**
   * 检测输入值是否满足期望值
   *
   * @param expected 期望值
   * @param given 输入值
   * @return 是否满足
   */
  fun check(expected: Double, given: Double): Boolean {
    return checkResult(given.compareTo(expected))
  }

  /**
   * 检测输入值是否满足期望值
   *
   * @param expected 期望值
   * @param given 输入值
   * @return 是否满足
   */
  fun <T : Comparable<T>> check(expected: T, given: T): Boolean {
    return checkResult(given.compareTo(expected))
  }

  protected abstract fun checkResult(compareResult: Int): Boolean

  @JsonValue
  override fun toString(): String {
    return when (this) {
      EQ -> "="
      NE -> "!="
      GT -> ">"
      LT -> "<"
      GE -> ">="
      LE -> "<="
    }
  }

  companion object {
    @JvmStatic
    @JsonCreator
    fun fromValue(value: String): CompareOperator {
      return when (value) {
        "=", "==" -> EQ
        "!=" -> NE
        ">" -> GT
        "<" -> LT
        ">=" -> GE
        "<=" -> LE
        else -> valueOf(value)
      }
    }
  }
}
