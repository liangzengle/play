package play.res.csv

import play.util.getRawClass
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

class CsvRow(private val header: CsvHeader, private val data: Array<Any>) {

  fun indexOf(columnName: String) = header.getColumnIndex(columnName)

  fun getBoolean(columnName: String): Boolean {
    val columnIndex = indexOf(columnName)
    return if (columnIndex == -1) false else data[columnIndex] as Boolean
  }

  fun getInt(columnName: String): Int {
    val columnIndex = indexOf(columnName)
    return if (columnIndex == -1) 0 else data[columnIndex] as Int
  }

  fun getLong(columnName: String): Long {
    val columnIndex = indexOf(columnName)
    return if (columnIndex == -1) 0 else data[columnIndex] as Long
  }

  fun getString(columnName: String): String {
    val columnIndex = indexOf(columnName)
    return if (columnIndex == -1) "" else data[columnIndex] as String
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> getList(columnName: String): List<T> {
    val columnIndex = indexOf(columnName)
    if (columnIndex == -1) return emptyList()
    val rawValue = data[columnIndex]!!
    val value = if (List::class.java.isAssignableFrom(rawValue.javaClass)) {
      rawValue as List<T>
    } else{
      listOf(rawValue) as List<T>
    }
    return java.util.List.copyOf(value)
  }

  fun <K : Any, V : Any> getMap(columnName: String, type: KType): Map<K, V> {
    return getMap(columnName, type.javaType as ParameterizedType)
  }

  @Suppress("UNCHECKED_CAST")
  fun <K : Any, V : Any> getMap(columnName: String, type: ParameterizedType): Map<K, V> {
    val keyColumnName = "$columnName.key"
    val valueColumnName = "$columnName.value"
    val keyIndex = indexOf(keyColumnName)
    val valueIndex = indexOf(valueColumnName)
    if (keyIndex == -1 && valueIndex == -1) {
      return emptyMap()
    }
    if (keyIndex == -1) {
      throw IllegalArgumentException("`$keyColumnName` not found")
    }
    if (valueIndex == -1) {
      throw IllegalArgumentException("`$valueColumnName` not found")
    }

    val rawKey = data[keyIndex]
    val rawValue = data[valueIndex]

    val keys = if (!List::class.java.isAssignableFrom(rawKey.javaClass)) {
      listOf(rawKey)
    } else {
      rawKey as List<Any>
    }

    val values = if (!List::class.java.isAssignableFrom(rawValue.javaClass)) {
      listOf(rawValue)
    } else {
      rawValue as List<Any>
    }

    if (keys.size < values.size) {
      throw IllegalStateException()
    }

    val actualTypeArguments = type.actualTypeArguments
    val keyType = actualTypeArguments[0].getRawClass<K>()
    val valueType = actualTypeArguments[1].getRawClass<V>()

    val entries = arrayOfNulls<Map.Entry<K, V>>(keys.size)
    for (i in keys.indices) {
      val key = keys[i]
      val value = if (values.size <= i) header.fieldTypes[valueIndex].defaultValue() else values[i]
      val k: K = keyType.cast(key)
      val v: V = valueType.cast(value)
      entries[i] = java.util.Map.entry(k, v)
    }
    return java.util.Map.ofEntries(*entries)
  }
}

