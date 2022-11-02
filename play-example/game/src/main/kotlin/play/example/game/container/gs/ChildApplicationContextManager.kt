package play.example.game.container.gs

import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ApplicationContextEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import play.example.game.container.gs.domain.GameServerId
import play.rsocket.metadata.RoutingMetadata
import play.rsocket.metadata.RoutingType
import play.rsocket.rpc.RpcClient
import play.rsocket.rpc.RpcClientInterceptor
import play.spring.SingletonBeanContext
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author LiangZengle
 */
@Component
class ChildApplicationContextManager : ApplicationListener<ApplicationContextEvent>, ApplicationContextAware,
  RpcClientInterceptor {
  private lateinit var parent: ApplicationContext

  private var contextMap = AtomicReference(IntObjectMaps.immutable.empty<SingletonBeanContext>())

  override fun onApplicationEvent(event: ApplicationContextEvent) {
    val context = event.applicationContext
    if (parent === context) {
      return
    }
    when (event) {
      is ContextRefreshedEvent -> addContext(context)
      is ContextClosedEvent -> removeContext(context)
    }
  }

  private fun addContext(context: ApplicationContext) {
    val gameServerId = context.getBean(GameServerId::class.java).toInt()
    contextMap.getAndUpdate {
      it.newWithKeyValue(gameServerId, SingletonBeanContext(context))
    }
  }

  private fun removeContext(context: ApplicationContext) {
    val gameServerId = context.getBean(GameServerId::class.java).toInt()
    contextMap.getAndUpdate {
      it.newWithoutKey(gameServerId)
    }
  }

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    parent = applicationContext
  }

  fun getContext(id: Int): SingletonBeanContext? {
    return contextMap.get().get(id)
  }

  fun getContexts(): Collection<SingletonBeanContext> = contextMap.get().values()

  override fun apply(rpcClient: RpcClient): RpcClient {
    return object : RpcClient {
      override fun <T : Any> getRpcService(serviceInterface: Class<T>, routing: RoutingMetadata): T {
        if (routing.routingType == RoutingType.UnicastToNode) {
          val nodeId = routing.nodeId
          val bean = getContext(nodeId)?.getBeanOrNull(serviceInterface)
          if (bean != null) {
            return bean
          }
        }
        return rpcClient.getRpcService(serviceInterface, routing)
      }
    }
  }
}
