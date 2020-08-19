package play.util.collection;

import play.util.function.LongBiFunction;
import play.util.function.LongToObjBiFunction;

import javax.annotation.Nullable;
import java.util.function.LongFunction;

public interface ConcurrentLongLongMap extends Iterable<ConcurrentLongLongMap.Entry> {

    @Nullable
    Long get(long key);

    long getOrDefault(long key, long defaultValue);

    @Nullable
    Long put(long key, long value);

    @Nullable
    Long putIfAbsent(long key, long value);

    @Nullable
    Long remove(long key);

    boolean remove(long key, long value);

    Long computeIfPresent(long key, LongBiFunction<Long> remappingFunction);

    Long computeIfAbsent(long key, LongFunction<Long> function);

    Long compute(long key, LongToObjBiFunction<Long, Long> remappingFunction);

    boolean containsKey(long key);

    boolean isEmpty();

    boolean isNotEmpty();

    int size();

    interface Entry {
        long getKey();

        long getValue();
    }
}
