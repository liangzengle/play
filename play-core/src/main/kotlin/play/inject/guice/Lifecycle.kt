package play.inject

import com.google.auto.service.AutoService
import com.google.inject.Module
import com.google.inject.Scopes
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
import play.Log
import play.ShutdownCoordinator
import javax.inject.Inject
import play.inject.guice.ContextualGuiceModule
import play.inject.guice.GuiceModule

interface PostConstruct {
  fun postConstruct()
}

interface PreDestroy {
  fun preDestroy()
}

@AutoService(Module::class)
class LifecycleGuiceModule : ContextualGuiceModule() {
  override fun configure() {
    bindListener(Matchers.any(), LifecycleBeanProvisionListener(shutdownCoordinator))
  }
}

internal class LifecycleBeanProvisionListener(shutdownCoordinator: ShutdownCoordinator) : ProvisionListener {

  private val destroyableBeans: MutableList<PreDestroy> = mutableListOf()

  init {
    shutdownCoordinator.addShutdownTask("Destroying bean") {
      synchronized(this) {
        destroyableBeans.sortBy {
          it.javaClass.getAnnotation(ShutdownCoordinator.Order::class.java)?.value
            ?: ShutdownCoordinator.PRIORITY_NORMAL
        }
        destroyableBeans.forEach {
          try {
            Log.info { "Destroying ${it.javaClass.simpleName}" }
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
    if (!Scopes.isSingleton(provisionInvocation.binding)) {
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
