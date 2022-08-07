package play.mvc

import play.codec.MessageCodec

/**
 *
 *
 * @author LiangZengle
 */
interface RequestBody {
  fun reset()
  fun readBoolean(): Boolean
  fun readInt(): Int
  fun readLong(): Long
  fun readString(): String
  fun getIntArray(): IntArray
  fun getLongArray(): LongArray
  fun getIntList(): List<Int>
  fun getLongList(): List<Long>

  fun getPayload(): ByteArray
  fun <T : Any> decodePayloadAs(type: Class<T>): T {
    return MessageCodec.decode(getPayload(), type)
  }
}
