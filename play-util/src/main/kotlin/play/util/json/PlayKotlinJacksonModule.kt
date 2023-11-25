package play.util.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.datatype.eclipsecollections.PackageVersion
import com.google.auto.service.AutoService

@AutoService(Module::class)
class PlayKotlinJacksonModule : SimpleModule() {
  override fun version(): Version {
    return PackageVersion.VERSION
  }

  override fun getModuleName(): String {
    return javaClass.simpleName
  }

  init {
    setDeserializerModifier(PlayDeserializerModifier())
  }

  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addSerializers(PlayKotlinJacksonSerializers())
    context.addDeserializers(PlayKotlinJacksonDeserializers())
  }
}

class PlayKotlinJacksonSerializers : Serializers.Base() {
  override fun findSerializer(
    config: SerializationConfig,
    javaType: JavaType,
    beanDesc: BeanDescription
  ): JsonSerializer<*>? {
    if (javaType.rawClass == IntRange::class.java) {
      return IntRangeSerializer
    }
    if (javaType.rawClass == LongRange::class.java) {
      return LongRangeSerializer
    }
    return null
  }
}


class PlayKotlinJacksonDeserializers : Deserializers.Base() {
  override fun findBeanDeserializer(
    type: JavaType, config: DeserializationConfig?, beanDesc: BeanDescription?
  ): JsonDeserializer<*>? {
    if (type.rawClass == IntRange::class.java) {
      return IntRangeDeserializer
    }
    if (type.rawClass == LongRange::class.java) {
      return LongRangeDeserializer
    }
    return null
  }
}
