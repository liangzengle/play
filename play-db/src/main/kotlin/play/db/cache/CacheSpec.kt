package play.db.cache

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CacheSpec(
  val initialSize: String = SIZE_DEFAULT,
  val loadAllOnInit: Boolean = false,
  val persistType: PersistType = PersistType.Scheduled,
  val expireEvaluator: KClass<out ExpireEvaluator> = DefaultExpireEvaluator::class
) {
  companion object {
    /**
     * 1
     */
    const val SIZE_ONE = "1"

    /**
     * 16
     */
    const val SIZE_MIN = "16"

    /**
     * app.db.cache.initial-size
     */
    const val SIZE_DEFAULT = "x1"
  }

  enum class PersistType {
    Scheduled,
    Manually,
  }
}
