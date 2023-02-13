package play.util.collection

import io.netty.util.AbstractConstant
import io.netty.util.ConstantPool
import java.lang.reflect.Type
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 *
 * @author LiangZengle
 */
class SerializableAttributeKey<T> private constructor(val id: Int, name: String) :
  AbstractConstant<SerializableAttributeKey<T>>(id, name) {

  private var type: Type = Void.TYPE

  fun valueType() = type

  companion object {
    @JvmStatic
    private val pool = KeyPool()

    @JvmStatic
    inline fun <reified T> valueOf(name: String): SerializableAttributeKey<T> {
      return valueOf(name, typeOf<T>().javaType)
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> valueOf(name: String, type: Type): SerializableAttributeKey<T> {
      return pool.valueOf(name, type) as SerializableAttributeKey<T>
    }

    @JvmStatic
    fun <T> valueOf(name: String, type: Class<T>): SerializableAttributeKey<T> {
      return valueOf(name, type as Type)
    }
  }

  private class KeyPool : ConstantPool<SerializableAttributeKey<Any>>() {
    override fun newConstant(id: Int, name: String): SerializableAttributeKey<Any> {
      return SerializableAttributeKey(id, name)
    }

    fun valueOf(name: String, type: Type): SerializableAttributeKey<Any> {
      val key = super.valueOf(name)
      val keyType = key.type
      if (keyType != Void.TYPE) {
        if (type != Void.TYPE && keyType != type) {
          throw IllegalArgumentException("Inconsistent type for key[$name]: $keyType, $type")
        }
      } else {
        key.type = type
      }
      return key
    }
  }
}
