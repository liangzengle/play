package play.entity.cache

import com.github.benmanes.caffeine.cache.Caffeine
import mu.KLogging
import play.entity.Entity
import play.entity.ObjId
import play.util.unsafeCast
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import kotlin.math.log

/**
 *
 * @author LiangZengle
 */
object EntityCacheHelper : KLogging() {

  @JvmStatic
  fun getInitialSizeOrDefault(entityClass: Class<*>, default: Int): Int {
    return entityClass.getAnnotation(InitialCacheSize::class.java)?.value?.let {
      when (it.first()) {
        'x', 'X' -> it.substring(1).toInt() * default
        else -> it.toInt()
      }
    } ?: default
  }

  @JvmStatic
  fun reportMissingInitialCacheSize(entityClass: Class<*>) {
    val shouldSpecifyInitialCacheSize = entityClass.getAnnotation(ShouldSpecifyInitialCacheSize::class.java) ?: return
    if (!shouldSpecifyInitialCacheSize.value) {
      return
    }
    val initialCacheSize = entityClass.getAnnotation(InitialCacheSize::class.java)
    if (initialCacheSize == null) {
      logger.warn { "Probably missing @${InitialCacheSize::class.simpleName} on ${entityClass.name}. You can use @${ShouldSpecifyInitialCacheSize::class.simpleName}(false) to suppress waring." }
    }
  }

  @JvmStatic
  private val multiKeyAccessorCache = Caffeine.newBuilder().build<Class<*>, MultiKeyAccessor<*>>(::newMultiIdAccessor)

  @JvmStatic
  private fun newMultiIdAccessor(idClass: Class<*>): MultiKeyAccessor<Any> {
    val l = MethodHandles.lookup()
    for (field in idClass.declaredFields) {
      if (field.isAnnotationPresent(MultiKey::class.java)) {
        try {
          val varHandle = l.findVarHandle(idClass, field.name, field.type)
          return MultiKeyAccessor(field.name, field.type.unsafeCast(), varHandle)
        } catch (e: IllegalAccessException) {
          throw IllegalStateException("field should be `public`: $field", e)
        }
      }
    }
    throw IllegalStateException("field annotated with @PartitionId is not found in ${idClass.name}")
  }

  internal fun <K> getMultiKeyAccessor(idClass: Class<out ObjId>): MultiKeyAccessor<K> {
    return multiKeyAccessorCache.get(idClass)!!.unsafeCast()
  }

  internal fun hasMultiKey(idClass: Class<out ObjId>): Boolean {
    return multiKeyAccessorCache.getIfPresent(idClass) != null
  }

  internal class MultiKeyAccessor<K>(
    private val name: String, private val type: Class<K>, private val handle: VarHandle
  ) {
    fun getValue(entity: Entity<*>): K {
      return handle.get(entity.id()).unsafeCast()
    }

    fun getKeyName() = name

    fun getKeyType() = type
  }
}

fun <K> Entity<out ObjId>.multiKey(): K = EntityCacheHelper.getMultiKeyAccessor<K>(this.id().javaClass).getValue(this)
