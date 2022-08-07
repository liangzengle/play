package play.rsocket.serializer.kryo

import play.rsocket.serializer.PlaySerializer
import play.rsocket.serializer.PlaySerializerProvider

/**
 *
 *
 * @author LiangZengle
 */
object KryoSerializerProvider : PlaySerializerProvider {
  @JvmStatic
  private val serializerThreadLocal = ThreadLocal.withInitial { KryoSerializer(PlayKryo.newInstance()) }

  override fun get(): PlaySerializer = serializerThreadLocal.get()
}
