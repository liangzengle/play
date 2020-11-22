package play.inject.guice

import com.google.inject.Scope
import com.google.inject.Scopes
import com.google.inject.Singleton
import com.google.inject.spi.BindingScopingVisitor
import com.google.inject.spi.ProvisionListener

/**
 *
 * @author LiangZengle
 */
fun <T : Any> isSingletonBinding(provisionInvocation: ProvisionListener.ProvisionInvocation<T>): Boolean {
  return provisionInvocation.binding.acceptScopingVisitor(
    object : BindingScopingVisitor<Boolean> {
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
    }
  )
}
