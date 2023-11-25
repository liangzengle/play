package play.util.collection

import com.fasterxml.jackson.annotation.JsonValue
import play.util.enumeration.Enums
import java.util.*
import java.util.stream.LongStream

/**
 *
 * @author LiangZengle
 */
class MutableEnumLongMap<K : Enum<K>>(keyType: Class<K>, initialValues: LongArray) {

  @Transient
  private val enumSet = EnumSet.allOf(keyType)

  @JsonValue
  private val values = LongArray(enumSet.size)

  init {
    if (initialValues.isNotEmpty()) {
      System.arraycopy(initialValues, 0, values, 0, initialValues.size)
    }
  }

  fun put(key: K, value: Long) {
    values[key.ordinal] = value
  }

  fun remove(key: K): Long {
    return values[key.ordinal]
  }

  fun get(key: K): Long {
    return values[key.ordinal]
  }

  fun clear() {
    values.fill(0)
  }

  fun forEach(action: (K, Long) -> Unit) {
    for (k in enumSet) {
      val value = values[k.ordinal]
      action(k, value)
    }
  }

  fun valuesStream(): LongStream = Arrays.stream(values)

  override fun toString(): String {
    val b = StringBuilder()
    b.append('{')
    for (k in enumSet) {
      val value = values[k.ordinal]
      if (value == 0L) continue
      if (k.ordinal != 0) b.append(',').append(' ')
      b.append(k.name).append('=').append(value)
    }
    b.append('}')
    return b.toString()
  }
}
