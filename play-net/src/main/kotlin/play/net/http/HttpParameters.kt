package play.net.http

import play.util.mapToObj
import play.util.primitive.toByteChecked
import play.util.primitive.toShortChecked
import java.util.*

abstract class HttpParameters {
  abstract fun getMulti(name: String): List<String>

  fun getString(name: String): String {
    return unwrap(getStringOptional(name), name)
  }

  abstract fun getStringOptional(name: String): Optional<String>

  fun getBooleanOptional(name: String): Optional<Boolean> {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return Optional.empty()
    }
    return try {
      Optional.of(opt.get().toBoolean())
    } catch (e: IllegalArgumentException) {
      throw ParameterFormatException(name, e)
    }
  }

  fun getBoolean(name: String): Boolean {
    return unwrap(getBooleanOptional(name), name)
  }

  fun getByte(name: String): Byte {
    return unwrap(getIntOptional(name), name).toByteChecked()
  }

  fun getShort(name: String): Short {
    return unwrap(getIntOptional(name), name).toShortChecked()
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

  fun getByteOptional(name: String): Optional<Byte> {
    return getIntOptional(name).mapToObj { it.toByteChecked() }
  }

  fun getShortOptional(name: String): Optional<Short> {
    return getIntOptional(name).mapToObj { it.toShortChecked() }
  }

  fun getIntOptional(name: String): OptionalInt {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return OptionalInt.empty()
    }
    return try {
      OptionalInt.of(opt.get().toInt())
    } catch (e: NumberFormatException) {
      throw ParameterFormatException(name, e)
    }
  }

  fun getLongOptional(name: String): OptionalLong {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return OptionalLong.empty()
    }
    return try {
      OptionalLong.of(opt.get().toLong())
    } catch (e: NumberFormatException) {
      throw ParameterFormatException(name, e)
    }
  }

  fun getDoubleOptional(name: String): OptionalDouble {
    val opt = getStringOptional(name)
    if (opt.isEmpty) {
      return OptionalDouble.empty()
    }
    return try {
      OptionalDouble.of(opt.get().toDouble())
    } catch (e: NumberFormatException) {
      throw ParameterFormatException(name, e)
    }
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun unwrap(option: OptionalInt, name: String): Int {
    return if (option.isPresent) option.asInt else throw ParameterMissionException(name)
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun unwrap(option: OptionalLong, name: String): Long {
    return if (option.isPresent) option.asLong else throw ParameterMissionException(name)
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun unwrap(option: OptionalDouble, name: String): Double {
    return if (option.isPresent) option.asDouble else throw ParameterMissionException(name)
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun <T> unwrap(option: Optional<T>, name: String): T {
    return if (option.isPresent) option.get() else throw ParameterMissionException(name)
  }
}
