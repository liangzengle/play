package play.db

@Suppress("UNCHECKED_CAST")
inline class ResultMap(private val map: Map<String, Any?>) {
  fun <T> getOrNull(key: String): T? = map[key] as T?

  fun getBoolean(key: String): Boolean {
    return when (val v = getObject(key)) {
      is Boolean -> v
      else -> throw ClassCastException("${v.javaClass} can not cast to Boolean")
    }
  }

  fun getInt(key: String): Int {
    return when (val v = getObject(key)) {
      is Int -> v.toInt()
      else -> throw ClassCastException("${v.javaClass} can not cast to Int")
    }
  }

  fun getLong(key: String): Long {
    return when (val v = getObject(key)) {
      is Int -> v.toLong()
      is Long -> v
      else -> throw ClassCastException("${v.javaClass} can not cast to Long")
    }
  }

  fun getDouble(key: String): Double {
    return when (val v = getObject(key)) {
      is Number -> v.toDouble()
      else -> throw ClassCastException("${v.javaClass} can not cast to Double")
    }
  }

  fun getNumber(key: String): Number {
    return when (val v = getObject(key)) {
      is Number -> v
      else -> throw ClassCastException("${v.javaClass} can not cast to Number")
    }
  }

  fun getString(key: String): String {
    return when (val v = getObject(key)) {
      is String -> v
      else -> throw ClassCastException("${v.javaClass} can not cast to Double")
    }
  }

  fun getObject(key: String): Any {
    return map[key] ?: throw NoSuchElementException(key)
  }

  override fun toString(): String {
    return map.toString()
  }
}
