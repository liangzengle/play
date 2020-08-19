package play.db.mongo.codec

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.BsonBinaryWriter
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.RawBsonDocument
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import play.db.Entity
import play.util.reflect.classOf

class EntityCodec<T : Entity<*>>(
  private val clazz: Class<T>,
  private val objectMapper: ObjectMapper,
  codecRegistry: CodecRegistry
) : Codec<T> {

  private val rawBsonDocumentCodec by lazy(LazyThreadSafetyMode.NONE) {
    codecRegistry.get(classOf<RawBsonDocument>())
  }

  override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
    val buffer = currentBuffer.get()
    try {
      objectMapper.writeValue(buffer, value)
      rawBsonDocumentCodec.encode(
        writer,
        RawBsonDocument(buffer.array(), 0, buffer.position),
        encoderContext
      )
    } finally {
      buffer.release()
    }
  }

  override fun getEncoderClass(): Class<T> {
    return clazz
  }

  override fun decode(reader: BsonReader, decoderContext: DecoderContext?): T {
    val buffer = currentBuffer.get()
    val writer = BsonBinaryWriter(buffer)
    try {
      writer.pipe(reader)
      return objectMapper.readValue(buffer.array(), clazz)
    } finally {
      buffer.release()
      writer.close()
    }
  }

  companion object {
    private val currentBuffer = ThreadLocal.withInitial { ByteArrayOutputBuffer() }
  }
}
