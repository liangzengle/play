package play.rsocket.serializer.kryo

import io.netty.util.concurrent.FastThreadLocal
import play.kryo.PlayKryoFactory
import play.rsocket.serializer.RSocketSerializer
import play.rsocket.serializer.RSocketSerializerProvider

/**
 *
 *
 * @author LiangZengle
 */
class KryoSerializerProvider(private val factory: PlayKryoFactory) : RSocketSerializerProvider {
  companion object {
    @JvmStatic
    private val ftl = FastThreadLocal<KryoSerializer>()
  }

  override fun get(): RSocketSerializer {
    var serializer = ftl.get()
    if (serializer == null) {
      serializer = KryoSerializer(factory.create())
      ftl.set(serializer)
    }
    return serializer
  }
}
