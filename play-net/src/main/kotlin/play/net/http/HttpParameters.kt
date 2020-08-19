package play.net.http

import io.vavr.control.Option

abstract class HttpParameters {
  abstract fun getMulti(name: String): List<String>

  fun getString(name: String): String {
    return unwrap(getStringOptional(name), name)
  }

  abstract fun getStringOptional(name: String): Option<String>

  fun getBooleanOptional(name: String): Option<Boolean> {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return Option.none()
    }
    return try {
      Option.some(opt.get()!!.toBoolean())
    } catch (e: IllegalArgumentException) {
      throw ParameterFormatException(name, e)
    }
  }

  fun getByteOptional(name: String): Option<Byte> {
    return getNumberOptional(name) { it.toByte() }
  }

  fun getShortOptional(name: String): Option<Short> {
    return getNumberOptional(name) { it.toShort() }
  }

  fun getIntOptional(name: String): Option<Int> {
    return getNumberOptional(name) { it.toInt() }
  }

  fun getLongOptional(name: String): Option<Long> {
    return getNumberOptional(name) { it.toLong() }
  }

  fun getDoubleOptional(name: String): Option<Double> {
    return getNumberOptional(name) { it.toDouble() }
  }

  fun getBoolean(name: String): Boolean {
    return unwrap(getBooleanOptional(name), name)
  }

  fun getByte(name: String): Byte {
    return unwrap(getByteOptional(name), name)
  }

  fun getShort(name: String): Short {
    return unwrap(getShortOptional(name), name)
  }

  fun getInt(name: String): Int {
    return unwrap(getIntOptional(name), name)
  }

  fun getLong(name: String): Long {
    return unwrap(getLongOptional(name), name)
  }

  fun getDouble(name: String): Double {
    return unwrap(getDoubleOptional(name), name)
  }

  private inline fun <T : Number> getNumberOptional(name: String, parser: (String) -> T): Option<T> {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return Option.none()
    }
    return try {
      Option.some(parser(opt.get()))
    } catch (e: NumberFormatException) {
      throw ParameterFormatException(name, e)
    }
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun <T> unwrap(option: Option<T>, name: String): T {
    return if (option.isDefined) option.get() else throw ParameterMissionException(name)
  }
}
