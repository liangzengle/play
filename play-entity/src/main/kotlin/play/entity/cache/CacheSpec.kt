package play.entity.cache

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Cache Specification
 *
 * @property loadAllOnInit if true, all entities will be loaded into cache on init
 * @property neverExpire if true, entities will never expire
 * @property expireAfterAccess how long to keep entities in cache after they are accessed, in seconds
 * @property persistInterval how often to persist entities to the database, in seconds
 * @property expireEvaluator determine whether an entity should be expired
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CacheSpec(
  val loadAllOnInit: Boolean = false,
  val neverExpire: Boolean = false,
  val expireAfterAccess: Int = 0,
  val persistInterval: Int = 0,
  val expireEvaluator: KClass<out ExpireEvaluator> = DefaultExpireEvaluator::class
)
