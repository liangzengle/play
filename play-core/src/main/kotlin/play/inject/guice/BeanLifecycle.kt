package play.inject.guice

import com.google.inject.spi.ProvisionListener
import play.ApplicationLifecycle
import play.Log

interface PostConstruct {
  fun postConstruct()
}

interface PreDestroy {
  fun preDestroy()
}

internal class LifecycleBeanManager(lifecycle: ApplicationLifecycle) : ProvisionListener {
  private val destroyableBeans: MutableList<PreDestroy> = mutableListOf()

  init {
    lifecycle.addShutdownHook("Destroying bean") {
      synchronized(this) {
        destroyableBeans.forEach {
          try {
            Log.info { "destroying ${it.javaClass.simpleName}" }
            it.preDestroy()
          } catch (e: Throwable) {
            Log.error(e) { e.message }
          }
        }
      }
    }
  }

  override fun <T : Any> onProvision(provisionInvocation: ProvisionListener.ProvisionInvocation<T>) {
    val bean = provisionInvocation.provision()
    if (!isSingletonBinding(provisionInvocation)) {
      if (bean is PostConstruct || bean is PreDestroy) {
        Log.warn { "[${bean.javaClass.name}]不是Singleton对象，不受Lifecycle管理" }
      }
      return
    }
    if (bean is PostConstruct) {
      try {
        bean.postConstruct()
      } catch (e: Exception) {
        Log.error(e) { "[${bean.javaClass.name}]初始化失败" }
        throw e
      }
    }
    if (bean is PreDestroy) {
      synchronized(this) {
        destroyableBeans.add(bean)
      }
    }
  }
}
