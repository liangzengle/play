package play

import com.google.inject.spi.ProvisionListener
import play.inject.guice.isSingletonBinding

/**
 * @author LiangZengle
 */
interface ApplicationEventListener

/**
 * 自动将bean注册到[ApplicationEventListener]
 */
internal class AutoRegisterProvisionListener(
  private val eventBus: ApplicationEventBus
) : ProvisionListener {
  override fun <T : Any> onProvision(provision: ProvisionListener.ProvisionInvocation<T>) {
    val bean = provision.provision()
    if (bean !is ApplicationEventListener) {
      return
    }
    if (!isSingletonBinding(provision)) {
      Log.warn { "[${bean.javaClass.name}]不是Singleton对象，需要手动注册" }
      return
    }
    Log.debug { "$eventBus registering [${bean.javaClass.name}]" }
    eventBus.register(bean)
  }
}
