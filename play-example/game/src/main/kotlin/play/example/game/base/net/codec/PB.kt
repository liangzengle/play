@file:JvmName("PB")

package play.example.game.base.net.codec

import kotlin.reflect.KClass
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import play.util.EmptyByteArray
import play.util.unsafeCast

object PB {

  @JvmStatic
  fun encode(o: Any?): ByteArray {
    return when (o) {
      null, Unit -> EmptyByteArray
      is ByteArray -> o
      else -> ProtoBuf.encodeToByteArray(o::class.serializer().unsafeCast(), o)
    }
  }

  @JvmStatic
  fun <T : Any> decode(bytes: ByteArray, type: KClass<T>) = ProtoBuf.decodeFromByteArray(type.serializer(), bytes)

  inline fun <reified T : Any> decode(bytes: ByteArray): T = ProtoBuf.decodeFromByteArray(bytes)
}
