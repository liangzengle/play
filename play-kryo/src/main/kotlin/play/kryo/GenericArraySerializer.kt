package play.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 *
 * @author LiangZengle
 */
class GenericArraySerializer<T> : Serializer<T>() {

  init {
    acceptsNull = false
    isImmutable = true
  }

  lateinit var componentType: Class<*>
  lateinit var componentSerializer: Serializer<Any>

  override fun write(kryo: Kryo, output: Output, `object`: T) {
    val array = `object` as Array<*>
    output.writeInt(array.size, true)
    for (element in array) {
      componentSerializer.write(kryo, output, element)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun read(kryo: Kryo, input: Input, type: Class<out T>?): T {
    val length = input.readInt(true)
    val array = java.lang.reflect.Array.newInstance(componentType, length) as Array<Any>
    for (index in array.indices) {
      val element = componentSerializer.read(kryo, input, componentType)
      array[index] = element
    }
    return array as T
  }
}
