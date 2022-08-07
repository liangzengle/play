package play.rsocket.broker.util

/**
 * @author LiangZengle
 */
interface IndexedMap<K, V, IDX> {
  fun get(k: K): V?
  fun put(k: K, v: V, idx: IDX): V?
  fun remove(k: K)
  fun remove(k: K, v: V)
  fun size(): Int
  fun isEmpty(): Boolean
  fun contains(k: K): Boolean
  fun clear()
  fun values(): Iterable<V>
  fun query(idx: IDX): Iterable<V>
}
