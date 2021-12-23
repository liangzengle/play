package play.example.game.container.rpc

import com.alibaba.rsocket.ServiceMapping
import com.alibaba.rsocket.metadata.GSVRoutingMetadata
import com.alibaba.rsocket.rpc.ReactiveMethodHandler
import com.alibaba.rsocket.utils.MurmurHash3
import com.alibaba.spring.boot.rsocket.RSocketProperties
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import mu.KLogging
import org.eclipse.collections.impl.factory.primitive.IntSets
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * @author LiangZengle
 */
class ContainerRSocketServiceAnnotationProcessor(rsocketProperties: RSocketProperties) :
  AbstractRSocketServiceAnnotationProcessor(rsocketProperties.group, rsocketProperties.version),
  BeanPostProcessor,
  GSVLocalReactiveServiceCaller {

  companion object : KLogging()

  private data class HandlerId(
    val group: String, val serviceName: String, val version: String, val handlerName: String
  )

  private var methodInvokeEntrances = hashMapOf<HandlerId, ReactiveMethodHandler>()
  private val rsocketHashCodeServices = IntSets.mutable.empty()
  private val handlerCache = Caffeine.newBuilder()
    .weakValues()
    .removalListener(RemovalListener<HandlerId, Any> { key, _, cause ->
      if (cause == RemovalCause.COLLECTED && key != null) {
        removeProvider(key.group, key.serviceName, key.version)
      }
    })
    .build<HandlerId, Any>()

  @Synchronized
  override fun addProvider(
    group: String, serviceName: String, version: String, serviceInterface: Class<*>, handler: Any
  ) {
    val serviceId = MurmurHash3.hash32(serviceName)
    rsocketHashCodeServices.add(serviceId)
    handlerCache.put(HandlerId(group, serviceName, version, ""), handler)

    for (method in serviceInterface.methods) {
      if (method.isDefault) continue
      val handlerName = method.getAnnotation(ServiceMapping::class.java)?.value?.ifEmpty { method.name } ?: method.name
      val key = HandlerId(group, serviceName, version, handlerName)
      methodInvokeEntrances[key] = ReactiveMethodHandler(serviceName, method, handler)
    }
    
    logger.debug { "rsocket rpc add provider: group=$group, serviceName=$serviceName, version=$version" }
  }

  override fun removeProvider(group: String, serviceName: String, version: String, serviceInterface: Class<*>) {
    removeProvider(group, serviceName, version)
  }

  @Synchronized
  private fun removeProvider(group: String, serviceName: String, version: String) {
    val iterator = methodInvokeEntrances.keys.iterator()
    while (iterator.hasNext()) {
      val handlerId = iterator.next()
      if (handlerId.group == group
        && handlerId.serviceName == serviceName
        && handlerId.version == version
      ) {
        iterator.remove()
        logger.debug { "rsocket rpc remove provider: group=$group, serviceName=$serviceName, version=$version" }
      }
    }
  }

  override fun getInvokeMethod(routing: GSVRoutingMetadata): ReactiveMethodHandler? {
    return methodInvokeEntrances[HandlerId(routing.group ?: "", routing.service, routing.version ?: "", routing.method)]
  }

  override fun getInvokeMethod(serviceName: String?, method: String?): ReactiveMethodHandler? {
    throw UnsupportedOperationException()
  }

  override fun getInvokeMethod(handlerId: Int?): ReactiveMethodHandler? {
    throw UnsupportedOperationException()
  }

  override fun contains(serviceName: String?, rpc: String?): Boolean {
    throw UnsupportedOperationException()
  }

  override fun contains(serviceName: String?): Boolean {
    throw UnsupportedOperationException()
  }

  override fun contains(serviceId: Int): Boolean {
    return rsocketHashCodeServices.contains(serviceId)
  }

  @Synchronized
  override fun findAllServices(): Set<String> {
    return methodInvokeEntrances.keys.asSequence().map { it.serviceName }.toSet()
  }

  override fun containsHandler(handlerId: Int?): Boolean {
    throw UnsupportedOperationException()
  }
}
