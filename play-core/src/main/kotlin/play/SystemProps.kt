package play

import java.util.*
import javax.annotation.Nonnull

object SystemProps {
  /**
   * 如果属性不存在则设置
   *
   * @param key   属性key
   * @param value 属性值
   * @return 原来的属性值，不存在为null
   */
  @JvmStatic
  fun setIfAbsent(key: String?, value: Any): String? {
    return System.getProperties().putIfAbsent(key, value.toString()) as String?
  }

  /**
   * 设置属性值
   *
   * @param key   属性key
   * @param value 属性值
   * @return 原来的属性值，不存在为null
   */
  @JvmStatic
  operator fun set(key: String?, value: Any): String? {
    return System.getProperties().put(key, value.toString()) as String?
  }

  @JvmStatic
  val all: Properties
    get() = System.getProperties()

  @JvmStatic
  fun getOrNull(key: String): String? {
    return System.getProperties().getProperty(key)
  }

  @JvmStatic
  fun getOrThrow(key: String): String {
    return System.getProperties().getProperty(key) ?: throw NoSuchElementException(key)
  }

  @JvmStatic
  fun getOrEmpty(key: String): String {
    return getOrDefault(key, "")
  }

  @JvmStatic
  fun getOrDefault(key: String?, @Nonnull defaultValue: String?): String {
    return System.getProperties().getProperty(key, defaultValue)
  }

  @JvmStatic
  operator fun get(key: String?): Optional<String> {
    return Optional.ofNullable(System.getProperties().getProperty(key))
  }

  @JvmStatic
  fun getInt(key: String): Int {
    return getOrThrow(key).toInt()
  }

  @JvmStatic
  fun getInt(key: String, defaultValue: Int): Int {
    val value = getOrNull(key)
    return value?.toInt() ?: defaultValue
  }

  @JvmStatic
  fun getDouble(key: String): Double {
    return getOrThrow(key).toDouble()
  }

  @JvmStatic
  fun getDouble(key: String, defaultValue: Double): Double {
    val value = getOrNull(key)
    return value?.toDouble() ?: defaultValue
  }

  @JvmStatic
  fun getBoolean(key: String): Boolean {
    return parseBoolean(getOrThrow(key))
  }

  @JvmStatic
  fun getBoolean(key: String, defaultValue: Boolean): Boolean {
    val value = getOrNull(key)
    return if (value == null) defaultValue else parseBoolean(value)
  }

  @JvmStatic
  private fun parseBoolean(value: String): Boolean {
    return when (value) {
      "1", "true", "on", "enable" -> true
      else -> false
    }
  }

  @JvmStatic
  fun osName(): String {
    return getOrEmpty("os.name")
  }

  @JvmStatic
  fun userDir(): String {
    return getOrEmpty("user.dir")
  }
}
