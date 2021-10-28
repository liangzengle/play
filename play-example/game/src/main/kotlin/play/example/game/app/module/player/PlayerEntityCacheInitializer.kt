package play.example.game.app.module.player

import org.springframework.stereotype.Component
import play.entity.cache.EntityCacheManager
import play.entity.cache.UnsafeEntityCacheOps
import play.example.game.app.module.player.entity.AbstractPlayerLongIdEntity
import play.util.reflect.ClassScanner
import play.util.unsafeCast

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
  private val playerEntityCaches: List<UnsafeEntityCacheOps<Long>> =
    classScanner.getOrdinarySubclasses(AbstractPlayerLongIdEntity::class.java)
      .map { entityCacheManager.get(it) }
      .filter { it is UnsafeEntityCacheOps<*> }
      .map { it.unsafeCast<UnsafeEntityCacheOps<Long>>() }
      .toList()

  /**
   * 将所有的玩家实体数据缓存置为空值，以减少1次无意义的数据库查询
   * @param id 为了使用包装类，声明为Long?
   */
  fun initWithEmptyValue(id: Long?) {
    for (i in playerEntityCaches.indices) {
      playerEntityCaches[i].initWithEmptyValue(id!!)
    }
  }
}
