package play.rsocket.rpc

import org.reactivestreams.Publisher
import play.rsocket.RequestType
import play.rsocket.util.ServiceUtil
import play.rsocket.util.Types
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 *
 * @author LiangZengle
 */
class RpcMethodMetadata(
  val serviceName: String,
  val methodName: String,
  val serviceId: Int,
  val methodId: Int,
  val returnDataType: Type,
  val requestType: RequestType
) {


  companion object {
    @JvmStatic
    fun of(method: Method): RpcMethodMetadata {
      if (!Publisher::class.java.isAssignableFrom(method.returnType)) {
        throw IllegalStateException("Must return a ${Publisher::class.qualifiedName}, e.g. Mono or Flux: $method")
      }
      val serviceId = ServiceUtil.getServiceId(method.declaringClass)
      val methodId = ServiceUtil.getMethodId(method)
      val returnDataType = when (val genericReturnType = method.genericReturnType) {
        is ParameterizedType -> genericReturnType.actualTypeArguments[0]
        is Class<*> -> genericReturnType
        else -> throw IllegalStateException("Unexpected return type: ${genericReturnType.typeName}")
      }
      val requestType = getRequestType(method)
      return RpcMethodMetadata(
        method.declaringClass.name,
        method.name,
        serviceId,
        methodId,
        returnDataType,
        requestType
      )
    }

    @JvmStatic
    fun of(function: KFunction<*>): RpcMethodMetadata {
      return of(function.javaMethod!!)
    }

    @JvmStatic
    private fun getRequestType(method: Method): RequestType {
      if (Types.isVoid(method.returnType)) {
        return RequestType.FireAndForget
      }
      val genericReturnType = method.genericReturnType
      if (genericReturnType is ParameterizedType) {
        if (genericReturnType.rawType == Mono::class.java) {
          return if (Types.isVoid(genericReturnType.actualTypeArguments[0])) {
            RequestType.FireAndForget
          } else {
            RequestType.RequestResponse
          }
        }
        if (genericReturnType.rawType == Flux::class.java) {
          val genericParameterTypes = method.genericParameterTypes
          val lastGenericParameterType = genericParameterTypes.last()
          return if (lastGenericParameterType is ParameterizedType && lastGenericParameterType.rawType == Flux::class.java) {
            if (Types.isVoid(lastGenericParameterType.actualTypeArguments[0])) {
              throw IllegalStateException("Can't use `void` Publisher as a parameter")
            }
            RequestType.RequestChannel
          } else {
            RequestType.RequestStream
          }
        }
      }
      throw IllegalStateException("Can't detect RequestType for method: $method")
    }
  }

  override fun toString(): String {
    return "RpcMethodMetadata(serviceName='$serviceName', methodName='$methodName')"
  }
}
