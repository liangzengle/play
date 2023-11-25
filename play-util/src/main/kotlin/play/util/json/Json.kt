package play.util.json

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
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
import kotlin.reflect.typeOf

/**
 * Created by LiangZengle on 2020/2/15.
 */
object Json {
  private val mapper = configure(ObjectMapper())

  private val prettyWriter = mapper.writer().withDefaultPrettyPrinter()

  @JvmStatic
  fun copyObjectMapper() = mapper.copy()

  @JvmStatic
  fun reader() = mapper.reader()

  @JvmStatic
  fun writer() = mapper.writer()

  @JvmStatic
  fun prettyWriter() = prettyWriter

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

  @JvmStatic
  fun stringify(value: Any): String = mapper.writeValueAsString(value)

  @JvmStatic
  fun toJsonByteArray(value: Any): ByteArray = mapper.writeValueAsBytes(value)

  @JvmStatic
  fun toJsonString(value: Any): String = stringify(value)

  inline fun <reified T> readValueAs(content: String): T {
    return reader().readValue(content.toJsonParser(), jacksonType<T>())
  }

  inline fun <reified T> jacksonType(): JavaType = typeOf<T>().toJavaType()

  /**
   * 获取指定字段的值，忽略子节点，如果不存在则返回null
   *
   * @param jsonObject json object的字符串
   * @param fieldName 字段名
   * @return
   */
  @JvmStatic
  fun getFieldText(jsonObject: String, fieldName: String): String? {
    return getFieldText(jsonObject, fieldName, true)
  }

  /**
   * 获取指定字段的值，如果不存在则返回null
   *
   * @param jsonObject json object的字符串
   * @param fieldName 字段名
   * @param skipChildren 是否忽略子节点
   * @return
   */
  @JvmStatic
  fun getFieldText(jsonObject: String, fieldName: String, skipChildren: Boolean): String? {
    jsonFactory().createParser(jsonObject).use {
      while (it.nextToken() != null) {
        if (it.currentToken == JsonToken.FIELD_NAME) {
          it.nextToken()
          if (it.currentName == fieldName) {
            return it.text
          } else if (skipChildren) {
            it.skipChildren()
          }
        }
      }
    }
    return null
  }

  @JvmStatic
  fun getElementText(jsonArray: String, index: Int): String? {
    jsonFactory().createParser(jsonArray).use {
      var i = 0
      var started = false
      while (it.nextToken() != null) {
        if (!started) {
          started = it.currentToken == JsonToken.START_ARRAY
          continue
        }
        if (i == index) {
          val startIndex = it.currentLocation.charOffset - 1
          it.skipChildren()
          val endIndex = it.currentLocation.charOffset
          return jsonArray.substring(startIndex.toInt(), endIndex.toInt())
        } else {
          i++
          it.skipChildren()
        }
      }
    }
    return null
  }
}
