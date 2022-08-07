package play.rsocket.util

import play.rsocket.rpc.RpcMethod
import play.rsocket.rpc.RpcServiceInterface
import java.lang.reflect.Method

/**
 *
 * @author LiangZengle
 */
object ServiceUtil {

  @JvmStatic
  fun getServiceId(serviceInterface: Class<*>): Int {
    val declaredServiceId = serviceInterface.getAnnotation(RpcServiceInterface::class.java)?.serviceId ?: 0
    return if (declaredServiceId != 0) declaredServiceId else Murmur3.hash32(serviceInterface.name)
  }

  @JvmStatic
  fun getMethodId(method: Method): Int {
    return method.getAnnotation(RpcMethod::class.java)?.value ?: Murmur3.hash32(method.name)
  }
}
