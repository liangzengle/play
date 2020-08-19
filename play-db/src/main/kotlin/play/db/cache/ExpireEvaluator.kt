package play.db.cache

import play.db.Entity
import javax.inject.Singleton

interface ExpireEvaluator {
  

  /**
   * 是否允许过期（从缓存中移除）
   */
  fun canExpire(entity: Entity<*>): Boolean
}

@Singleton
internal class DefaultExpireEvaluator : ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean = true
}

@Singleton
class NeverExpireEvaluator : ExpireEvaluator {
  override fun canExpire(entity: Entity<*>): Boolean = false
}
