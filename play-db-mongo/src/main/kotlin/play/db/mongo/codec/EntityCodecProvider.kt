package play.db.mongo.codec

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import play.db.Entity
import play.util.reflect.isAssignable
import play.util.unsafeCast

class EntityCodecProvider(private val objectMapper: ObjectMapper) : CodecProvider {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
    if (!isAssignable<Entity<*>>(clazz)) return null
    return EntityCodec(clazz.unsafeCast<Class<Entity<*>>>(), objectMapper, registry) as Codec<T>
  }
}
