package play.example.common.gm

import play.example.module.player.Self
import play.util.collection.mkString
import play.util.reflect.invokeUnchecked
import java.lang.reflect.Method

class GmCommandInvoker(private val method: Method, private val target: Any) {
  init {
    method.trySetAccessible()
  }

  private val parameters = method.parameters.drop(1)
  private val parameterConverters = parameters.map { ParameterConverter.invoke(it.type) }

  fun invoke(self: Self, args: List<String>): Any? {
    val params = Array<Any?>(parameters.size + 1) { null }
    params[0] = self
    for (i in parameters.indices) {
      val parameter = parameters[i]
      val arg: String
      if (args.size <= i) {
        arg = parameter.getAnnotation(GmCommandArg::class.java)?.defaultValue ?: ""
        if (arg.isEmpty()) {
          throw GmCommandArgMissingException("缺少第${i + 1}个参数")
        }
      } else {
        arg = args[i]
      }
      try {
        params[i + 1] = parameterConverters[i].convert(parameter, arg)
      } catch (e: IllegalArgumentException) {
        throw GmCommandIllegalArgException("第${i + 1}个参数错误")
      }
    }
    return method.invokeUnchecked(target, *params)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GmCommandInvoker

    if (method != other.method) return false

    return true
  }

  override fun hashCode(): Int {
    return method.hashCode()
  }

  override fun toString(): String {
    return "${target.javaClass.simpleName}.${method.name}(Self, ${parameters.mkString(',') { it.type.name }}"
  }
}
