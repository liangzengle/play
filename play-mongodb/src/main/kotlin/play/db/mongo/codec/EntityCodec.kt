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
import org.bson.io.BasicOutputBuffer
import play.entity.Entity
import play.util.classOf

class EntityCodec<T : Entity<*>>(
  private val clazz: Class<T>,
  private val objectMapper: ObjectMapper,
  codecRegistry: CodecRegistry
) : Codec<T> {

  private val rawBsonDocumentCodec by lazy(LazyThreadSafetyMode.NONE) {
    codecRegistry.get(classOf<RawBsonDocument>())
  }

  override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
    val buffer = bufferThreadLocal.get()
    buffer.use {
      objectMapper.writeValue(it, value)
      val doc = RawBsonDocument(it.internalBuffer, 0, buffer.position)
      rawBsonDocumentCodec.encode(writer, doc, encoderContext)
    }
  }

  override fun getEncoderClass(): Class<T> {
    return clazz
  }

  override fun decode(reader: BsonReader, decoderContext: DecoderContext?): T {
    val buffer = bufferThreadLocal.get()
    val writer = BsonBinaryWriter(buffer)
    try {
      writer.pipe(reader)
      return objectMapper.readValue(buffer.internalBuffer, 0, buffer.position, clazz)
    } finally {
      buffer.close() // never fail
      writer.close()
    }
  }

  companion object {
    private val bufferThreadLocal:ThreadLocal<BasicOutputBuffer> = ThreadLocal.withInitial { ReusableOutputBuffer() }
  }
}
