package play.db.mongo.codec

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.util.concurrent.FastThreadLocal
import org.bson.BsonBinaryWriter
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.RawBsonDocument
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.io.BasicOutputBuffer
import play.util.classOf
import play.util.unsafeLazy

class JacksonBsonCodec<T>(
  private val clazz: Class<T>,
  private val objectMapper: ObjectMapper,
  codecRegistry: CodecRegistry
) : Codec<T> {

  private val rawBsonDocumentCodec by unsafeLazy {
    codecRegistry.get(classOf<RawBsonDocument>())
  }

  override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
    val buffer = bufferThreadLocal.get()
    buffer.use {
      objectMapper.writeValue(it, value)
      val doc = RawBsonDocument(it.internalBuffer, 0, it.position)
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
    private val bufferThreadLocal: FastThreadLocal<BasicOutputBuffer> =
      object : FastThreadLocal<BasicOutputBuffer>() {
        override fun initialValue(): BasicOutputBuffer {
          return ReusableOutputBuffer()
        }
      }
  }
}
