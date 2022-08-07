package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.serializers.CollectionSerializer
import play.rsocket.util.Types
import java.lang.reflect.ParameterizedType

/**
 *
 * @author LiangZengle
 */
class CollectionSerializerResolver : ParameterizedTypeSerializerResolver<Collection<*>> {

  @Suppress("UNCHECKED_CAST")
  override fun getSerializer(kryo: PlayKryo, type: ParameterizedType): Serializer<Collection<*>> {
    val rawType = type.rawType as Class<*>
    val typeArguments = type.actualTypeArguments
    val elementType = typeArguments[0]
    val serializer = kryo.getRegistration(rawType, true).serializer
    if (serializer is CollectionSerializer<*>) {
      serializer.isImmutable = true
      serializer.elementClass = Types.getRawClass(elementType)
      serializer.elementSerializer = kryo.getSerializer(elementType)
    }
    return serializer as Serializer<Collection<*>>
  }
}
