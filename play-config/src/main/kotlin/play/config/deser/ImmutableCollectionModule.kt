package play.config.deser

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.datatype.guava.deser.*
import java.util.*

/**
 * Created by LiangZengle on 2020/2/23.
 */
internal class ImmutableCollectionModule : SimpleModule() {
  override fun setupModule(context: SetupContext) {
    context.addDeserializers(ImmutableCollectionDeserializers())
  }
}

internal class ImmutableCollectionDeserializers : Deserializers.Base() {
  override fun findMapDeserializer(
    type: MapType,
    config: DeserializationConfig?,
    beanDesc: BeanDescription?,
    keyDeserializer: KeyDeserializer?,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<*>? {
    val rawClass = type.rawClass
    if (SortedMap::class.java.isAssignableFrom(rawClass)) {
      return ImmutableSortedMapDeserializer(type, keyDeserializer, elementDeserializer, elementTypeDeserializer, null);
    }
    if (Map::class.java.isAssignableFrom(rawClass)) {
      return ImmutableMapDeserializer(type, keyDeserializer, elementDeserializer, elementTypeDeserializer, null);
    }
    return null;
  }

  override fun findCollectionDeserializer(
    type: CollectionType,
    config: DeserializationConfig?,
    beanDesc: BeanDescription?,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<*>? {
    val rawClass = type.rawClass
    if (SortedSet::class.java.isAssignableFrom(rawClass)) {
      return ImmutableSortedSetDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    if (Set::class.java.isAssignableFrom(rawClass)) {
      return ImmutableSetDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    if (List::class.java.isAssignableFrom(rawClass)) {
      return ImmutableListDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    return null;
  }
}

