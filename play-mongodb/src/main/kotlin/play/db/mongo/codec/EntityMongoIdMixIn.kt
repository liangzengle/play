package play.db.mongo.codec

import com.fasterxml.jackson.annotation.JsonProperty

abstract class MongoLongIdMixIn(@field:JsonProperty("_id") val id: Long)

abstract class MongoIntIdMixIn(@field:JsonProperty("_id") val id: Int)

abstract class MongoStringIdMixIn(@field:JsonProperty("_id") val id: String)
