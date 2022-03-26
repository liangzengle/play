package play.util.collection

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*
import java.util.stream.IntStream

/**
 *
 * @author LiangZengle
 */
class MutableEnumIntMap<K : Enum<K>>(keyType: Class<K>, initialValues: IntArray) {

  @Transient
  private val enumSet = EnumSet.allOf(keyType)

  @JsonValue
  private val values = IntArray(enumSet.size)

  init {
    if (initialValues.isNotEmpty()) {
      System.arraycopy(initialValues, 0, values, 0, initialValues.size)
    }
  }

  fun put(key: K, value: Int) {
    values[key.ordinal] = value
  }

  fun remove(key: K): Int {
    return values[key.ordinal]
  }

  fun get(key: K): Int {
    return values[key.ordinal]
  }

  fun clear() {
    values.fill(0)
  }

  fun forEach(action: (K, Int) -> Unit) {
    for (k in enumSet) {
      val value = values[k.ordinal]
      action(k, value)
    }
  }

  fun valuesStream(): IntStream = Arrays.stream(values)

  override fun toString(): String {
    val b = StringBuilder()
    b.append('{')
    for (k in enumSet) {
      val value = values[k.ordinal]
      if (value == 0) continue
      if (k.ordinal != 0) b.append(',').append(' ')
      b.append(k.name).append('=').append(value)
    }
    b.append('}')
    return b.toString()
  }
}
