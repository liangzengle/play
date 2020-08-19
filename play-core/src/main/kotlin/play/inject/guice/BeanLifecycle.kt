package play.inject.guice

import com.google.inject.Scope
import com.google.inject.Scopes
import com.google.inject.Singleton
import com.google.inject.spi.BindingScopingVisitor
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
    if (!shouldManage(provisionInvocation)) {
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

  private fun <T : Any> shouldManage(provisionInvocation: ProvisionListener.ProvisionInvocation<T>): Boolean {
    return provisionInvocation.binding.acceptScopingVisitor(object : BindingScopingVisitor<Boolean> {
      override fun visitScopeAnnotation(scopeAnnotation: Class<out Annotation>?): Boolean {
        return scopeAnnotation == Singleton::class.java || scopeAnnotation == javax.inject.Singleton::class.java
      }

      override fun visitScope(scope: Scope?): Boolean {
        return scope == Scopes.SINGLETON
      }

      override fun visitNoScoping(): Boolean {
        return false
      }

      override fun visitEagerSingleton(): Boolean {
        return true
      }
    })
  }
}
