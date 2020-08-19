package play.util.collection;

import play.util.function.LongToObjBiFunction;

import javax.annotation.Nullable;
import java.util.function.LongFunction;

public interface ConcurrentLongObjectMap<V> extends Iterable<ConcurrentLongObjectMap.Entry<V>> {

    V get(long key);

    V getOrDefault(long key, V defaultValue);

    V put(long key, V value);

    @Nullable
    V putIfAbsent(long key, V value);

    @Nullable
    V remove(long key);

    boolean remove(long key, V value);

    V computeIfPresent(long key, LongToObjBiFunction<? super V, ? extends V> remappingFunction);

    V computeIfAbsent(long key, LongFunction<? extends V> function);

    V compute(long key, LongToObjBiFunction<? super V, ? extends V> remappingFunction);

    boolean containsKey(long key);

    boolean isEmpty();

    boolean isNotEmpty();

    int size();

    interface Entry<V> {
        long getKey();

        V getValue();
    }
}
