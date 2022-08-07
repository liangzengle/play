package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultClassResolver

class FactoryRegistration(
  private val kryo: Kryo,
  type: Class<*>,
  private val factory: SerializerFactory<*>,
  id: Int = DefaultClassResolver.NAME.toInt()
) : Registration(type, DummySerializer, id) {
  companion object {
    @JvmStatic
    private val DummySerializer = object : Serializer<Any>() {
      override fun write(kryo: Kryo?, output: Output?, `object`: Any?) {
        throw UnsupportedOperationException()
      }

      override fun read(kryo: Kryo?, input: Input?, type: Class<out Any>?): Any {
        throw UnsupportedOperationException()
      }
    }
  }

  override fun getSerializer(): Serializer<*> {
    val serializer = super.getSerializer()
    if (serializer !== DummySerializer) {
      return serializer
    }
    return factory.newSerializer(kryo, type)
  }
}
