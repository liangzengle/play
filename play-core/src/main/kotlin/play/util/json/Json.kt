package play.util.json

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URL

/**
 * Created by LiangZengle on 2020/2/15.
 */
object Json {
  var mapper = ObjectMapper().useDefaultConfiguration()
    private set

  fun replaceDefaultMapper(newMapper: ObjectMapper) {
    mapper = newMapper
  }

  fun configureDefault(mapper: ObjectMapper): ObjectMapper {
    return mapper.useDefaultConfiguration()
  }

  fun ObjectMapper.useDefaultConfiguration(): ObjectMapper {
    return this.findAndRegisterModules()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
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

  inline fun <reified E> to(content: String): E = mapper.readValue(content)
  inline fun <reified E> to(src: ByteArray): E = mapper.readValue(src)
  inline fun <reified E> to(src: URL): E = mapper.readValue(src)
  inline fun <reified E> to(src: InputStream): E = mapper.readValue(src)

  fun stringify(value: Any): String = mapper.writeValueAsString(value)

  fun <T : Any> T.toJsonString(): String = stringify(this)
}
