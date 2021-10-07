package play.example.game.container.gm

import play.util.collection.mkString
import play.util.reflect.Reflect
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class GmCommandInvoker(private val method: Method, private val target: Any, commanderType: Class<*>) {

  private val parameters: List<Parameter>
  private val parameterConverters: List<ParameterConverter<*>>

  init {
    method.trySetAccessible()
    val params = method.parameters
    check(params[0].type == commanderType) { "GM指令方法的第一个参数必须是[${commanderType.name}]: $method" }
    parameters = params.drop(1)
    parameterConverters = parameters.map { ParameterConverter.invoke(it.type) }
  }

  fun invoke(commander: Any, args: List<String>): Any? {
    val params = arrayOfNulls<Any?>(parameters.size + 1)
    params[0] = commander
    for (i in parameters.indices) {
      val paramIdx = i + 1
      val parameter = parameters[i]
      val arg: String =
        if (args.size <= i) {
          val defaultValue = parameter.getAnnotation(GmCommandModule.Arg::class.java)?.defaultValue
          if (defaultValue.isNullOrEmpty()) {
            throw GmCommandArgMissingException("缺少第${paramIdx}个参数: $this $args")
          }
          defaultValue
        } else {
          args[i]
        }
      try {
        params[paramIdx] = parameterConverters[i].convert(parameter, arg)
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
    val firstParamTypeName = method.parameterTypes[0].simpleName
    val otherParamTypeNames = parameters.mkString(',') { it.type.simpleName }
    return "${target.javaClass.simpleName}.${method.name}($firstParamTypeName, $otherParamTypeNames)"
  }
}
