package play.entity.cache

import play.entity.LongIdEntity
import play.entity.ObjId
import play.entity.ObjIdEntity

/**
 *
 * @author LiangZengle
 */
class MyEntity(id: Long, @CacheIndex val playerId: Long) : LongIdEntity(id)

data class MyObjId(@CacheIndex val playerId: Long, val heroId: Int) : ObjId()

class MyObjIdEntity(id: MyObjId) : ObjIdEntity<MyObjId>(id)
