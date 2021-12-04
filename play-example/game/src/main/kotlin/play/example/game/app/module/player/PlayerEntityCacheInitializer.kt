package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.entity.cache.EntityCacheManager
import play.entity.cache.UnsafeEntityCacheOps
import play.example.game.app.module.player.entity.AbstractPlayerEntity
import play.util.collection.toImmutableList
import play.util.isAssignableFrom
import play.util.reflect.ClassScanner

/**
 * 玩家实体数据缓存初始化
 *
 * @author LiangZengle
 */
@Component
class PlayerEntityCacheInitializer(
  classScanner: ClassScanner,
  entityCacheManager: EntityCacheManager
) {

  /**
   * 实现了[UnsafeEntityCacheOps]接口的所有玩家实体类缓存
   */
  private val playerEntityCaches: List<UnsafeEntityCacheOps<Long>> = entityCacheManager.getAllCaches()
    .asSequence()
    .filter { isAssignableFrom<AbstractPlayerEntity>(it.entityClass) }
    .filterIsInstance<UnsafeEntityCacheOps<Long>>()
    .toImmutableList()

  /**
   * 玩家注册时，将所有的玩家实体数据缓存置为空值，以减少1次无意义的数据库查询
   */
  fun initWithEmptyValue(id: Long) {
    @Suppress("RedundantNullableReturnType")
    val boxedId: Long? = id
    val entityCaches = playerEntityCaches
    for (i in entityCaches.indices) {
      entityCaches[i].initWithEmptyValue(boxedId!!)
    }
  }
}
