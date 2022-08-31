package play.kryo

import com.esotericsoftware.kryo.Serializer
import java.lang.reflect.ParameterizedType

/**
 *
 * @author LiangZengle
 */
interface ParameterizedTypeSerializerResolver<T> {

  fun getSerializer(kryo: PlayKryo, type: ParameterizedType): Serializer<T>
}

internal object DefaultSerializerResolver : ParameterizedTypeSerializerResolver<Any> {
  override fun getSerializer(kryo: PlayKryo, type: ParameterizedType): Serializer<Any> {
    return kryo.getSerializer(type.rawType as Class<*>)
  }
}
