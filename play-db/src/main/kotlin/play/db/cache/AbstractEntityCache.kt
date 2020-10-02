package play.db.cache

import play.db.Entity
import play.getLogger
import play.inject.Injector


abstract class AbstractEntityCache<ID : Any, E : Entity<ID>>(
  protected val entityClass: Class<E>,
  protected val injector: Injector
) : EntityCache<ID, E> {

  companion object {
    @JvmStatic
    protected val logger = getLogger()

    @JvmStatic
    protected val NOOP: (Any) -> Any? = { null }
  }

  protected val expireEvaluator: ExpireEvaluator

  init {
    val annotation = entityClass.getAnnotation(CacheSpec::class.java)
    expireEvaluator = if (annotation != null && annotation.expireEvaluator != DefaultExpireEvaluator::class) {
      injector.getInstance(annotation.expireEvaluator.java)
    } else {
      injector.getInstance(DefaultExpireEvaluator::class.java)
    }
  }

  override fun entityClass(): Class<E> {
    return entityClass
  }

  protected fun getInitialSizeOrDefault(default: Int): Int {
    return entityClass.getAnnotation(CacheSpec::class.java)?.initialSize?.let {
      when (it.first()) {
        'x', 'X' -> it.substring(1).toInt() * default
        else -> it.toInt()
      }
    } ?: default
  }

  protected fun isLoadAllOnInit() = entityClass.getAnnotation(CacheSpec::class.java)?.loadAllOnInit ?: false
}
