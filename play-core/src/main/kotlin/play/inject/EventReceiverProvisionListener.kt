package play.inject

import com.google.common.eventbus.EventBus
import com.google.inject.Scopes
import com.google.inject.spi.ProvisionListener
import play.ApplicationEventBus
import play.ApplicationEventListener

/**
 * 自动将bean注册到[eventBus]
 */
@Suppress("UnstableApiUsage")
internal class EventReceiverProvisionListener(
  private val eventBus: EventBus
) : ProvisionListener {

  override fun <T : Any> onProvision(provision: ProvisionListener.ProvisionInvocation<T>) {
    val bean = provision.provision()
    if (bean !is ApplicationEventListener) {
      return
    }
    if (!Scopes.isSingleton(provision.binding)) {
      ApplicationEventBus.logger.warn { "[${bean.javaClass.name}]不是Singleton对象，需要手动注册" }
      return
    }
    ApplicationEventBus.logger.debug { "$eventBus registering [${bean.javaClass.name}]" }
    eventBus.register(bean)
  }
}
