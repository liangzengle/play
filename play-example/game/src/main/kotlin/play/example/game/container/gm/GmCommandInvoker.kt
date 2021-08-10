package play.example.game.container.gm

import play.example.game.app.module.player.Self
import play.util.collection.mkString
import play.util.reflect.Reflect
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class GmCommandInvoker(private val method: Method, private val target: Any) {

  private val parameters: List<Parameter>
  private val parameterConverters: List<ParameterConverter<*>>

  init {
    method.trySetAccessible()
    val params = method.parameters
    check(params[0].type == Self::class.java) { "第一个参数必须是[${Self::class.simpleName}]: $method" }
    parameters = params.drop(1)
    parameterConverters = parameters.map { ParameterConverter.invoke(it.type) }
  }

  fun invoke(self: Self, args: List<String>): Any? {
    val params = arrayOfNulls<Any?>(parameters.size + 1)
    params[0] = self
    for (i in parameters.indices) {
      val parameter = parameters[i]
      val arg: String = if (args.size <= i) {
        val defaultValue = parameter.getAnnotation(GmCommandModule.Arg::class.java)?.defaultValue
        if (defaultValue.isNullOrEmpty()) {
          throw GmCommandArgMissingException("缺少第${i + 1}个参数: $this $args")
        }
        defaultValue
      } else {
        args[i]
      }
      try {
        params[i + 1] = parameterConverters[i].convert(parameter, arg)
      } catch (e: IllegalArgumentException) {
        throw GmCommandIllegalArgException("第${i + 1}个参数错误: $this $args")
      }
    }
    return Reflect.invokeMethod(method, target, *params)
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
    return "${target.javaClass.simpleName}.${method.name}(Self, ${parameters.mkString(',') { it.type.simpleName }}"
  }
}
