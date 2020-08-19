package play.util.collection;

import play.util.function.ObjLongFunction;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

public interface ConcurrentObjectLongMap<K> extends Iterable<ConcurrentObjectLongMap.Entry<K>> {

    Long get(K key);

    long getOrDefault(K key, long defaultValue);

    Long put(K key, long value);

    @Nullable
    Long putIfAbsent(K key, long value);

    @Nullable
    Long remove(K key);

    boolean remove(K key, long value);

    Long computeIfPresent(K key, ObjLongFunction<K, Long> remappingFunction);

    Long computeIfAbsent(K key, ToLongFunction<K> function);

    Long compute(K key, BiFunction<K, Long, Long> remappingFunction);

    boolean containsKey(K key);

    boolean isEmpty();

    boolean isNotEmpty();

    int size();

    interface Entry<K> {
        Long getKey();

        long getValue();
    }
}
