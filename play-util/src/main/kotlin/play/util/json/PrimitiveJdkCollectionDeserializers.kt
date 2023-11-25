package play.util.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.datatype.eclipsecollections.PackageVersion

class PrimitiveJdkCollectionModule : Module() {
  override fun version(): Version = PackageVersion.VERSION

  override fun getModuleName(): String = "PrimitiveCollection"

  override fun setupModule(context: SetupContext) {
    context.addDeserializers(PrimitiveJdkCollectionDeserializers())
  }
}

internal class PrimitiveJdkCollectionDeserializers : Deserializers.Base() {
  override fun findCollectionDeserializer(
    type: CollectionType,
    config: DeserializationConfig?,
    beanDesc: BeanDescription?,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<*>? {
    return PrimitiveMutableCollectionDeserializers.forCollectionType(type)
  }

  override fun findMapDeserializer(
    type: MapType,
    config: DeserializationConfig?,
    beanDesc: BeanDescription?,
    keyDeserializer: KeyDeserializer?,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<*>? {
    return PrimitiveMutableCollectionDeserializers.forMapType(type)
  }
}


