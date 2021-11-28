package play.entity.cache

import play.entity.ObjId
import play.entity.ObjIdEntity

/**
 *
 * @author LiangZengle
 */
class MyEntity(id: MyObjId) :
  ObjIdEntity<MyObjId>(id) {

  constructor(playerId: Long, itemId: Int) : this(MyObjId(playerId, itemId))
}

data class MyObjId(@JvmField @MultiKey val playerId: Long, val itemId: Int) : ObjId()
