package play.rsocket.rpc

/**
 *
 * @author LiangZengle
 */
interface LocalServiceCallerRegistry {

  fun find(serviceId: Int): LocalServiceCaller?

  fun register(service: LocalServiceCaller)

  fun register(services: Collection<LocalServiceCaller>)
}
