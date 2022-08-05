package play.codec.protobuf

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import play.codec.Codec
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 *
 *
 * @author LiangZengle
 */
class ProtobufCodec<T : MessageLite>(type: Class<T>) : Codec<T> {
  @Suppress("UNCHECKED_CAST")
  private val parser =
    MethodHandles.publicLookup().findStatic(type, "getParser", MethodType.methodType(Parser::class.java, type))
      .invokeExact() as Parser<T>


  override fun encode(value: T?): ByteArray {
    return value?.toByteArray() ?: ByteArray(0)
  }

  override fun decode(bytes: ByteArray): T {
    return parser.parseFrom(bytes)
  }
}
