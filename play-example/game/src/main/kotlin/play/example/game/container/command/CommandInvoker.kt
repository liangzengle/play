package play.example.game.container.command

import play.util.collection.mkString
import play.util.reflect.Reflect
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class CommandInvoker(private val method: Method) {

  private val parameters: List<Parameter>
  private val parameterConverters: List<ParameterConverter<*>>

  val targetClass: Class<*> get() = method.declaringClass

  init {
    method.trySetAccessible()
    parameters = method.parameters.asList()
    parameterConverters = parameters.map { ParameterConverter.invoke(it.type) }
  }

  fun invoke(commander: Any, target: Any, args: List<String>): Any? {
    val params = arrayOfNulls<Any?>(parameters.size + 1)
    for (i in parameters.indices) {
      val parameter = parameters[i]
      if (i == 0 && parameter.type.isAssignableFrom(commander.javaClass)) {
        params[i] = commander
      }
      val arg: String =
        if (args.size <= i) {
          val defaultValue = parameter.getAnnotation(Param::class.java)?.defaultValue
          if (defaultValue.isNullOrEmpty()) {
            throw CommandParamMissingException("缺少第${i}个参数: $this $args")
          }
          defaultValue
        } else {
          args[i]
        }
      try {
        params[i] = parameterConverters[i].convert(parameter, arg)
      } catch (e: IllegalArgumentException) {
        throw CommandParamIllegalException("第${i}个参数错误: $this $args")
      }
    }
    return Reflect.invokeMethod(method, target, *params)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CommandInvoker

    if (method != other.method) return false

    return true
  }

  override fun hashCode(): Int {
    return method.hashCode()
  }

  override fun toString(): String {
    val firstParamTypeName = method.parameterTypes[0].simpleName
    val otherParamTypeNames = parameters.mkString(',') { it.type.simpleName }
    return "${method.declaringClass.simpleName}.${method.name}($firstParamTypeName, $otherParamTypeNames)"
  }
}
