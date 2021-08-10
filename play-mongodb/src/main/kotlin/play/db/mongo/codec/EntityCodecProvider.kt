package play.db.mongo.codec

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import play.entity.Entity
import play.entity.ObjId
import play.util.isAssignableFrom

class EntityCodecProvider(private val objectMapper: ObjectMapper) : CodecProvider {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
    if (isAssignableFrom<Entity<*>>(clazz) || isAssignableFrom<ObjId>(clazz)) {
      return JacksonBsonCodec(clazz, objectMapper, registry)
    }
    return null
  }
}
