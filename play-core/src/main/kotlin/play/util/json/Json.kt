package play.util.json

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URL
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * Created by LiangZengle on 2020/2/15.
 */
object Json {
  private val mapper = configure(ObjectMapper())

  @JvmStatic
  fun copyObjectMapper() = mapper.copy()

  @JvmStatic
  fun reader() = mapper.reader()

  @JvmStatic
  fun writer() = mapper.writer()

  @JvmStatic
  fun jsonFactory() = mapper.factory

  @JvmStatic
  fun typeFactory() = mapper.typeFactory

  @JvmStatic
  fun configure(mapper: ObjectMapper): ObjectMapper {
    mapper.findAndRegisterModules()
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

    ServiceLoader.load(ObjectMapperConfigurator::class.java).forEach {
      it.configure(mapper)
    }
    return mapper
  }

  @JvmStatic
  fun String.toJsonParser(): JsonParser = mapper.createParser(this)

  @JvmStatic
  fun ByteArray.toJsonParser(): JsonParser = mapper.createParser(this)

  @JvmStatic
  fun URL.toJsonParser(): JsonParser = mapper.createParser(this)

  @JvmStatic
  fun InputStream.toJsonParser(): JsonParser = mapper.createParser(this)

  @JvmStatic
  fun Type.toJavaType(): JavaType = mapper.typeFactory.constructType(this)

  @JvmStatic
  fun KType.toJavaType(): JavaType = mapper.typeFactory.constructType(this.javaType)

  @JvmStatic
  fun TypeReference<*>.toJavaType(): JavaType = mapper.typeFactory.constructType(this)

  @JvmStatic
  fun <T> convert(fromValue: Any, toValueType: Class<T>): T = mapper.convertValue(fromValue, toValueType)

  @JvmStatic
  fun <T> convert(fromValue: Any, targetType: Type): T = mapper.convertValue(fromValue, targetType.toJavaType())

  @JvmStatic
  fun <T> convert(fromValue: Any, targetType: JavaType): T = mapper.convertValue(fromValue, targetType)

  @JvmStatic
  fun <T> convert(fromValue: Any, targetType: TypeReference<T>): T = mapper.convertValue(fromValue, targetType)

  @JvmStatic
  fun <T> convert(fromValue: Any, type: KType): T = convert(fromValue, type.toJavaType())

  @JvmStatic
  fun <T> toObject(content: String, type: Class<T>): T {
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <T> toObject(parser: JsonParser, type: Class<T>): T {
    return mapper.readValue(parser, type)
  }

  @JvmStatic
  fun <T> toObject(content: String, type: Type): T {
    return mapper.readValue(content, type.toJavaType())
  }

  @JvmStatic
  fun <T> toObject(content: String, type: KType): T {
    return mapper.readValue(content, type.toJavaType())
  }

  @JvmStatic
  fun <T> toObject(content: String, type: TypeReference<T>): T {
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <T> toObject(content: String, type: JavaType): T {
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <T> toList(parser: JsonParser, elemType: Class<T>): List<T> {
    val type = mapper.typeFactory.constructCollectionType(List::class.java, elemType)
    return mapper.readValue(parser, type)
  }

  @JvmStatic
  fun <T> toList(content: String, elemType: Class<T>): List<T> {
    val type = mapper.typeFactory.constructCollectionType(List::class.java, elemType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <T> toSet(content: String, elemType: Class<T>): Set<T> {
    val type = mapper.typeFactory.constructCollectionType(Set::class.java, elemType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <T> toSet(parser: JsonParser, elemType: Class<T>): Set<T> {
    val type = mapper.typeFactory.constructCollectionType(Set::class.java, elemType)
    return mapper.readValue(parser, type)
  }

  @JvmStatic
  fun <K, V> toMap(content: String, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(content, type)
  }

  @JvmStatic
  fun <K, V> toMap(parse: JsonParser, keyType: Class<K>, valueType: Class<V>): Map<K, V> {
    val type = mapper.typeFactory.constructMapType(Map::class.java, keyType, valueType)
    return mapper.readValue(parse, type)
  }

  fun stringify(value: Any): String = mapper.writeValueAsString(value)

  fun toJsonByteArray(value: Any): ByteArray = mapper.writeValueAsBytes(value)

  fun toJsonString(value: Any): String = stringify(value)
}
