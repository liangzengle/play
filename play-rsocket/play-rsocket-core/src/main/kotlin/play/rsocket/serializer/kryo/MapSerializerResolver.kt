package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.serializers.MapSerializer
import play.rsocket.util.Types
import java.lang.reflect.ParameterizedType

/**
 *
 * @author LiangZengle
 */
class MapSerializerResolver : ParameterizedTypeSerializerResolver<Map<*, *>> {

  @Suppress("UNCHECKED_CAST")
  override fun getSerializer(kryo: PlayKryo, type: ParameterizedType): Serializer<Map<*, *>> {
    val typeArguments = type.actualTypeArguments
    val rawType = type.rawType as Class<*>
    val keyType = typeArguments[0]
    val valueType = typeArguments[1]
    val serializer = kryo.getRegistration(rawType, true).serializer
    if (serializer is MapSerializer) {
      serializer.isImmutable = true
      serializer.setKeysCanBeNull(false)
      serializer.setValuesCanBeNull(false)
      serializer.setKeyClass(Types.getRawClass(keyType))
      serializer.setValueClass(Types.getRawClass(valueType))
      serializer.setKeySerializer(kryo.getSerializer(keyType))
      serializer.setValueSerializer(kryo.getSerializer(valueType))
    }
    return serializer as Serializer<Map<*, *>>
  }
}
