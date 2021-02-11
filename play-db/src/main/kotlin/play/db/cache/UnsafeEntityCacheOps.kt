package play.db.cache

/**
 * 不安全的缓存操作
 *
 * @author LiangZengle
 */
interface UnsafeEntityCacheOps<ID> {
  /**
   * 强制将[id]对应的缓存的entity初始化为空值。在确定entity不存在时，先初始化为空值再创建可减少1次无意义的数据库查询
   *
   * @param id ID
   */
  fun initWithEmptyValue(id: ID)

  /**
   * 删除
   *
   * @param id ID
   */
  fun deleteUnprotected(id: ID)
}
