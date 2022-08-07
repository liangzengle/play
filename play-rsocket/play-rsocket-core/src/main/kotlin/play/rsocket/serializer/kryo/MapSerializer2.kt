package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.serializers.MapSerializer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentNavigableMap
import java.util.concurrent.ConcurrentSkipListMap

class MapSerializer2<T : Map<*, *>> : MapSerializer<T>(), SerializerFactory<MapSerializer2<T>> {

  init {
    acceptsNull = false
    isImmutable = true
    setKeysCanBeNull(false)
    setValuesCanBeNull(false)
  }

  @Suppress("UNCHECKED_CAST")
  override fun create(kryo: Kryo, input: Input?, type: Class<out T>?, size: Int): T {
    return when (type) {
      Map::class.java -> hashMapOf<Any, Any>() as T
      NavigableMap::class.java -> TreeMap<Any, Any>() as T
      ConcurrentMap::class.java -> ConcurrentHashMap<Any, Any>() as T
      NavigableMap::class.java, SortedMap::class.java -> TreeMap<Any, Any>() as T
      ConcurrentNavigableMap::class.java -> ConcurrentSkipListMap<Any, Any>() as T
      else -> super.create(kryo, input, type, size)
    }
  }

  override fun newSerializer(kryo: Kryo, type: Class<*>): MapSerializer2<T> {
    return MapSerializer2()
  }

  override fun isSupported(type: Class<*>?): Boolean {
    return true
  }
}
