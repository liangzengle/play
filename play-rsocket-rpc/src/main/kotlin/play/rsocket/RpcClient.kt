package play.rsocket.rpc

import com.alibaba.rsocket.ServiceLocator
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder
import com.alibaba.rsocket.upstream.UpstreamManager
import com.github.benmanes.caffeine.cache.Caffeine
import play.util.unsafeCast
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
class RpcClient private constructor(
  private val upstreamManager: UpstreamManager,
  cacheBuilder: Caffeine<SimpleServiceId, Any>
) {

  private val serviceCache = cacheBuilder.build<SimpleServiceId, Any>()

  companion object {
    private val groupHandle: VarHandle
    private val versionHandle: VarHandle
    private val serviceHandle: VarHandle

    init {
      val lookup = MethodHandles.lookup()
      groupHandle = lookup.findVarHandle(RSocketRemoteServiceBuilder::class.java, "group", String::class.java)
      versionHandle = lookup.findVarHandle(RSocketRemoteServiceBuilder::class.java, "version", String::class.java)
      serviceHandle = lookup.findVarHandle(RSocketRemoteServiceBuilder::class.java, "service", String::class.java)
    }

    private fun getServiceId(b: RSocketRemoteServiceBuilder<*>): SimpleServiceId {
      val group = groupHandle.get(b) as String?
      val version = versionHandle.get(b) as String?
      val service = serviceHandle.get(b) as String
      return SimpleServiceId(group, version, service)
    }

    @JvmStatic
    fun create(
      upstreamManager: UpstreamManager,
      cacheCustomizer: Caffeine<SimpleServiceId, Any>.() -> Unit
    ): RpcClient {
      val builder = Caffeine.newBuilder().unsafeCast<Caffeine<SimpleServiceId, Any>>()
      cacheCustomizer(builder)
      Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build<SimpleServiceId, Any>()
      return RpcClient(upstreamManager, builder)
    }

    @JvmStatic
    fun create(
      upstreamManager: UpstreamManager
    ): RpcClient {
      return create(upstreamManager) {
        expireAfterAccess(Duration.ofHours(1))
      }
    }
  }

  fun <T : Any> getService(serviceType: Class<T>): T {
    return getService(serviceType, null, null)
  }

  fun <T : Any> getService(serviceType: Class<T>, group: String?): T {
    return getService(serviceType, group, null)
  }

  fun <T : Any> getService(serviceType: Class<T>, group: String?, version: String?): T {
    return getService(serviceType) {
      group?.also(this::group)
      version?.also(this::version)
    }
  }

  fun <T> getService(serviceType: Class<T>, init: RSocketRemoteServiceBuilder<T>.() -> Unit): T {
    val builder = RSocketRemoteServiceBuilder.client(serviceType).upstreamManager(upstreamManager).apply(init)
    val serviceId = getServiceId(builder)
    val service = serviceCache.getIfPresent(serviceId)
    if (service != null) {
      return service.unsafeCast()
    }
    return serviceCache.asMap().computeIfAbsent(serviceId) { builder.build() }.unsafeCast()
  }


  data class SimpleServiceId(
    @JvmField val group: String?,
    @JvmField val version: String?,
    @JvmField val serviceName: String
  ) {
    override fun toString(): String {
      return ServiceLocator.serviceId(group, serviceName, version)
    }
  }
}
