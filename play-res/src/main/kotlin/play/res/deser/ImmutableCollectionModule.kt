package play.res.deser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.impl.FieldProperty
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.datatype.guava.deser.*
import play.res.IllegalResourceFieldTypeException
import play.util.json.PrimitiveImmutableCollectionDeserializers
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
  ): JsonDeserializer<*> {
    val rawClass = type.rawClass
    val primitiveMapDeserializer = PrimitiveImmutableCollectionDeserializers.forMapType(type)
    if (primitiveMapDeserializer != null) {
      return primitiveMapDeserializer
    }
    if (NavigableMap::class.java == rawClass || SortedMap::class.java == rawClass) {
      return ImmutableSortedMapDeserializer(type, keyDeserializer, elementDeserializer, elementTypeDeserializer, null)
    }
    if (Map::class.java == rawClass) {
      return ImmutableMapDeserializer(type, keyDeserializer, elementDeserializer, elementTypeDeserializer, null)
    }
    return ImpossibleDeserializer(type)
  }

  override fun findCollectionDeserializer(
    type: CollectionType,
    config: DeserializationConfig?,
    beanDesc: BeanDescription?,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<*> {
    val rawClass = type.rawClass
    val primitiveMapDeserializer = PrimitiveImmutableCollectionDeserializers.forCollectionType(type)
    if (primitiveMapDeserializer != null) {
      return primitiveMapDeserializer
    }
    if (SortedSet::class.java == rawClass || NavigableSet::class.java == rawClass) {
      return ImmutableSortedSetDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    if (Set::class.java == rawClass) {
      return ImmutableSetDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    if (List::class.java == rawClass) {
      return ImmutableListDeserializer(type, elementDeserializer, elementTypeDeserializer, null, null)
    }
    return ImpossibleDeserializer(type)
  }
}

private class ImpossibleDeserializer(javaType: JavaType) : StdDeserializer<Any>(javaType), ContextualDeserializer {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any {
    throw IllegalStateException()
  }

  override fun createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer<*> {
    if (property !is FieldProperty) {
      throw IllegalStateException("")
    }
    val member = property.member.member
    throw IllegalResourceFieldTypeException("集合字段类型应声明为接口类型: ${member.declaringClass.name}.${member.name}")
  }
}
