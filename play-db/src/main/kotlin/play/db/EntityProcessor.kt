package play.db

/**
 * 实体从数据加载后需要执行的操作
 * @author LiangZengle
 */
abstract class EntityProcessor<T : Entity<*>> {

  open fun postLoad(entity: T) {
    entity.postLoad()
  }
}

internal object DefaultEntityProcessor : EntityProcessor<Entity<*>>()
