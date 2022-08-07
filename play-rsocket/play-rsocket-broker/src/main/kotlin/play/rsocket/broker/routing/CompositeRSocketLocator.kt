package play.rsocket.broker.routing

import io.rsocket.RSocket
import play.rsocket.broker.rsocket.UnknownRSocket
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType

/**
 *
 * @author LiangZengle
 */
class CompositeRSocketLocator(private val locators: List<RSocketLocator>) : RSocketLocator {

  override fun supports(routingType: RoutingType): Boolean {
    for (i in locators.indices) {
      if (locators[i].supports(routingType)) {
        return true
      }
    }
    return false
  }

  override fun locate(routing: RoutingMetadata): RSocket {
    for (i in locators.indices) {
      val locator = locators[i]
      if (locator.supports(routing.routingType)) {
        return locator.locate(routing)
      }
    }
    return UnknownRSocket(routing)
  }
}
