package play.rsocket.serializer.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.serializers.CollectionSerializer
import java.util.*

class CollectionSerializer2<T : Collection<*>> : CollectionSerializer<T>(),
  SerializerFactory<CollectionSerializer2<T>> {

  init {
    acceptsNull = false
    isImmutable = true
  }

  @Suppress("UNCHECKED_CAST")
  override fun create(kryo: Kryo?, input: Input?, type: Class<out T>?, size: Int): T {
    return when (type) {
      List::class.java, Collection::class.java -> return ArrayList<Any>(size) as T
      Set::class.java -> return HashSet<Any>(size) as T
      NavigableSet::class.java, SortedSet::class.java -> return TreeSet<Any>() as T
      else -> super.create(kryo, input, type, size)
    }
  }

  override fun newSerializer(kryo: Kryo?, type: Class<*>?): CollectionSerializer2<T> {
    return CollectionSerializer2()
  }

  override fun isSupported(type: Class<*>?): Boolean {
    return true
  }
}
