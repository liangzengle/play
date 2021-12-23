package play.example.common.rpc

import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder
import com.alibaba.rsocket.upstream.UpstreamManager
import com.github.benmanes.caffeine.cache.Caffeine
import play.util.unsafeCast
import java.time.Duration
import kotlin.reflect.KClass

/**
 *
 * @author LiangZengle
 */
class RpcClient(private val upstreamManager: UpstreamManager) {

  private val serviceCache = Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build<String, Any>()

  fun <T : Any> getService(kclass: KClass<T>): T {
    val key = kclass.qualifiedName!!
    val service = serviceCache.getIfPresent(key)
    if (service != null) {
      return service.unsafeCast()
    }
    return serviceCache.asMap().computeIfAbsent(key) {
      RSocketRemoteServiceBuilder
        .client(kclass.java)
        .upstreamManager(upstreamManager)
        .build()
    }.unsafeCast()
  }

  fun <T : Any> getService(kclass: KClass<T>, sid: Int): T {
    val key = "${kclass.qualifiedName}:$sid"
    val service = serviceCache.getIfPresent(key)
    if (service != null) {
      return service.unsafeCast()
    }
    return serviceCache.asMap().computeIfAbsent(key) {
      RSocketRemoteServiceBuilder
        .client(kclass.java)
        .group(sid.toString())
        .upstreamManager(upstreamManager)
        .build()
    }.unsafeCast()
  }

}
