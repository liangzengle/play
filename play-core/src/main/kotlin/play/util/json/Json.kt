package play.util.json

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URL
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * Created by LiangZengle on 2020/2/15.
 */
object Json {
  var mapper = configure(ObjectMapper())
    private set

  @JvmStatic
  fun replaceDefaultMapper(newMapper: ObjectMapper) {
    mapper = newMapper
  }

  @JvmStatic
  fun configure(mapper: ObjectMapper): ObjectMapper {
    return mapper.findAndRegisterModules()
      .registerModule(PrimitiveJdkCollectionModule())
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.SETTER, Visibility.NONE)
  }

  @JvmStatic
  fun <E> toObject(content: String, type: Class<E>): E {
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <E> toObject(content: String, type: Type): E {
    return mapper.readValue(content, mapper.typeFactory.constructType(type))
  }

  @JvmStatic
  fun <E> toList(content: String, elemType: Class<E>): List<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(List::class.java, elemType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <E> toList(src: ByteArray, elemType: Class<E>): List<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(List::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <E> toList(src: URL, elemType: Class<E>): List<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(List::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <E> toList(src: InputStream, elemType: Class<E>): List<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(List::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <E> toSet(content: String, elemType: Class<E>): Set<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(Set::class.java, elemType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <E> toSet(src: ByteArray, elemType: Class<E>): Set<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(Set::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <E> toSet(src: URL, elemType: Class<E>): Set<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(Set::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <E> toSet(src: InputStream, elemType: Class<E>): Set<E> {
    val type = mapper.typeFactory.constructCollectionLikeType(Set::class.java, elemType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <K, V> toMap(content: String, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <K, V> toMap(src: ByteArray, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <K, V> toMap(src: URL, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <K, V> toMap(src: InputStream, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(src, type)
  }

  @JvmStatic
  fun <T> convert(fromValue: Any, toValueType: Class<T>): T = mapper.convertValue(fromValue, toValueType)

  @JvmStatic
  fun <T> convert(fromValue: Any, targetType: Type): T = mapper.convertValue(fromValue, targetType.toJavaType())

  @JvmStatic
  inline fun <reified T> convert(fromValue: Any): T = mapper.convertValue(fromValue, typeOf<T>().javaType.toJavaType())

  inline fun <reified E> to(content: String): E = mapper.readValue(content)
  inline fun <reified E> to(src: ByteArray): E = mapper.readValue(src)
  inline fun <reified E> to(src: URL): E = mapper.readValue(src)
  inline fun <reified E> to(src: InputStream): E = mapper.readValue(src)

  fun stringify(value: Any): String = mapper.writeValueAsString(value)

  fun <T : Any> T.toJsonString(): String = stringify(this)

  fun Type.toJavaType(): JavaType = mapper.typeFactory.constructType(this)
}
