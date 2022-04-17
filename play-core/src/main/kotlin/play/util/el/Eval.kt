package play.util.el

import com.github.benmanes.caffeine.cache.Caffeine
import org.mvel2.MVEL
import org.mvel2.integration.VariableResolverFactory
import play.util.unsafeCast
import java.io.Serializable

/**
 *
 * @author LiangZengle
 */
object Eval {

  private val compiledExpressions = Caffeine.newBuilder().softValues().build<String, Serializable>()

  fun compile(expr: String): Serializable {
    return compiledExpressions.get(expr, MVEL::compileExpression)
  }

  /**
   * 获取缓存的公式数量
   */
  fun cachedExprCount() = compiledExpressions.estimatedSize()

  /**
   * 没有变量的公式求值
   *
   * @param expr 公式
   * @return Result
   */
  fun eval(expr: String): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), null as? Map<*, *>?) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param vars 变量值
   * @return Result
   */
  fun eval(expr: String, vars: Map<String, Any>): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), null, vars) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param ctx 上下文（提供所需的函数）
   * @return Result
   */
  fun eval(expr: String, ctx: Any): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), ctx, null as? Map<*, *>?) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param vars 变量值
   * @param ctx 上下文（提供所需的函数）
   * @return Result
   */
  fun eval(expr: String, vars: Map<String, Any>, ctx: Any): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), ctx, vars) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param factory VariableResolverFactory
   * @return Result
   */
  fun eval(expr: String, factory: VariableResolverFactory): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), factory) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param ctx 上下文（提供所需的函数）
   * @param factory VariableResolverFactory
   * @return Result
   */
  fun eval(expr: String, ctx: Any, factory: VariableResolverFactory): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), ctx, factory) })
  }

  /**
   * 公式求值
   *
   * @param expr 公式
   * @param vars 变量值列表，依次对应变量: n1, n2, ...
   * @return Result
   */
  fun evalWithVars(expr: String, vararg vars: Any): Result {
    return Result(kotlin.Result.runCatching { MVEL.executeExpression(compile(expr), VarMap(vars)) })
  }

  class Result(private val result: kotlin.Result<Any>) {
    fun getBooleanOrThrow(): Boolean = result.getOrThrow().unsafeCast()
    fun getBooleanOrDefault(default: Boolean): Boolean = result.getOrDefault(default).unsafeCast()

    fun getNumberOrThrow(): Number = result.getOrThrow().unsafeCast()
    fun getNumberOrDefault(default: Number): Number = result.getOrDefault(default).unsafeCast()

    fun getIntOrThrow(): Int = getNumberOrThrow().toInt()
    fun getIntOrDefault(default: Int): Int = getNumberOrDefault(default).toInt()

    fun getLongOrThrow(): Long = getNumberOrThrow().toLong()
    fun getLongOrDefault(default: Long): Long = getNumberOrDefault(default).toLong()

    fun getDoubleOrThrow(): Double = getNumberOrThrow().toDouble()
    fun getDoubleOrDefault(default: Double): Double = getNumberOrDefault(default).toDouble()

    fun <R : Any> getOrThrow(): R = result.getOrThrow().unsafeCast()
    fun <R : Any> getOrDefault(default: R): R = result.getOrDefault(default).unsafeCast()

    override fun toString(): String {
      return result.toString()
    }
  }

  private class VarMap(private val vars: Array<out Any>) : Map<String, Any> {
    override val entries: Set<Map.Entry<String, Any>>
      get() = throw UnsupportedOperationException()
    override val keys: Set<String>
      get() = throw UnsupportedOperationException()
    override val size: Int
      get() = vars.size
    override val values: Collection<Any>
      get() = throw UnsupportedOperationException()

    override fun containsKey(key: String): Boolean {
      return get(key) != null
    }

    override fun containsValue(value: Any): Boolean {
      throw UnsupportedOperationException()
    }

    override fun get(key: String): Any? {
      var n = 0
      for (index in key.indices) {
        if (index == 0) {
          if (key[index] == 'n') {
            continue
          } else {
            return null
          }
        } else {
          if (key[index].isLetter()) {
            return null
          } else {
            val v = key[index] - '0'
            n = n * 10 + v
          }
        }
      }
      return vars.getOrNull(n - 1)
    }

    override fun isEmpty(): Boolean = vars.isEmpty()
  }
}
