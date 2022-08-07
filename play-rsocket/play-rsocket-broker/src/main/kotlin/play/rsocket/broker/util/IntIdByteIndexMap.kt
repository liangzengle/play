package play.rsocket.broker.util

import io.netty.util.collection.ByteObjectHashMap
import io.netty.util.collection.ByteObjectMap
import io.netty.util.collection.IntObjectHashMap
import io.netty.util.collection.IntObjectMap
import org.jctools.maps.NonBlockingHashMapLong
import org.jctools.maps.NonBlockingSetInt
import java.util.*

/**
 *
 * @author LiangZengle
 */
class IntIdByteIndexMap<V : Any> : IndexedMap<Int, V, Byte> {

  private val idToValue = NonBlockingHashMapLong<V>()
  private val idToIndex: IntObjectMap<Byte> = IntObjectHashMap()
  private val indexToId: ByteObjectMap<NonBlockingSetInt> = ByteObjectHashMap()

  override fun get(k: Int): V? {
    return idToValue[k.toLong()]
  }

  override fun put(k: Int, v: V, idx: Byte): V? {
    synchronized(this) {
      val prev = idToValue.put(k.toLong(), v)
      val prevIndex = idToIndex.remove(k)
      if (prevIndex != null) {
        indexToId[prevIndex]?.remove(k)
      }
      idToIndex.put(k, idx)
      indexToId.getOrPut(idx) { NonBlockingSetInt() }.add(k)
      return prev
    }
  }

  override fun remove(k: Int) {
    synchronized(this) {
      idToValue.remove(k.toLong())
      val index = idToIndex.remove(k)
      if (index != null) {
        indexToId.get(index)?.remove(k)
      }
    }
  }

  override fun remove(k: Int, v: V) {
    synchronized(this) {
      val success = idToValue.remove(k.toLong(), v as Any)
      if (success) {
        val index = idToIndex.remove(k)
        if (index != null) {
          indexToId.get(index)?.remove(k)
        }
      }
    }
  }

  override fun size(): Int {
    return idToValue.size
  }

  override fun isEmpty(): Boolean {
    return idToValue.isEmpty()
  }

  override fun contains(k: Int): Boolean {
    return idToValue.containsKey(k.toLong())
  }

  override fun clear() {
    synchronized(this) {
      idToValue.clear()
      idToIndex.clear()
      indexToId.clear()
    }
  }

  override fun values(): Iterable<V> {
    return Collections.unmodifiableCollection(idToValue.values)
  }

  override fun query(idx: Byte): Iterable<V> {
    val idSet = indexToId[idx] ?: emptyList()
    return idSet.asSequence().mapNotNull(::get).asIterable()
  }
}
