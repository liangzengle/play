package play.rsocket.rpc

import io.netty.util.collection.IntObjectHashMap
import io.netty.util.collection.IntObjectMap
import play.rsocket.util.ServiceUtil

/**
 *
 * @author LiangZengle
 */
class LocalServiceCallerRegistryImpl(serviceCallers: List<LocalServiceCaller>) : LocalServiceCallerRegistry {
  private var idToService: IntObjectMap<LocalServiceCaller> = IntObjectHashMap()

  init {
    register(serviceCallers)
  }

  override fun find(serviceId: Int): LocalServiceCaller? {
    return idToService[serviceId]
  }

  override fun register(service: LocalServiceCaller) {
    register(listOf(service))
  }

  @Synchronized
  override fun register(services: Collection<LocalServiceCaller>) {
    for (service in services) {
      val serviceId = ServiceUtil.getServiceId(service.serviceInterface())
      val prev = idToService.get(serviceId)
      if (prev != null) {
        throw IllegalStateException("Duplicated serviceId $serviceId in both ${prev.javaClass.name} and ${service.javaClass.name}")
      }
      idToService.put(serviceId, service)
    }
  }
}
