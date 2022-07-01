package play.entity.cache

import play.entity.LongIdEntity

/**
 *
 * @author LiangZengle
 */
class MyEntity(id: Long, @CacheIndex val playerId: Long) : LongIdEntity(id)
