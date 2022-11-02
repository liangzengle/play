package play.rsocket.rpc

import io.netty.util.collection.IntObjectHashMap
import io.netty.util.collection.IntObjectMap
import play.rsocket.util.ServiceUtil

/**
 *
 * @author LiangZengle
 */
class ImmutableLocalServiceCallerRegistry(serviceCallers: List<LocalServiceCaller>) : LocalServiceCallerRegistry {
  private val idToService: IntObjectMap<LocalServiceCaller> = IntObjectHashMap(serviceCallers.size)

  init {
    for (service in serviceCallers) {
      val serviceId = ServiceUtil.getServiceId(service.serviceInterface())
      val prev = idToService.get(serviceId)
      if (prev != null) {
        throw IllegalStateException("Duplicated serviceId $serviceId in both ${prev.javaClass.name} and ${service.javaClass.name}")
      }
      idToService.put(serviceId, service)
    }
  }

  override fun find(serviceId: Int): LocalServiceCaller? {
    return idToService[serviceId]
  }

  override fun register(service: LocalServiceCaller) {
    throw UnsupportedOperationException()
  }

  override fun register(services: Collection<LocalServiceCaller>) {
    throw UnsupportedOperationException()
  }
}
