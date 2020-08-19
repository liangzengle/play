package play.db.mongo.codec

import com.fasterxml.jackson.annotation.JsonProperty
import play.db.mongo.Mongo.ID
import play.entity.ObjId

abstract class MongoLongIdMixIn(@field:JsonProperty(ID) val id: Long)

abstract class MongoIntIdMixIn(@field:JsonProperty(ID) val id: Int)

abstract class MongoObjIdMixIn(@field:JsonProperty(ID) val id: ObjId)
