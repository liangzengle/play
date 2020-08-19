package play.util.function;

@FunctionalInterface
public interface ObjLongFunction<T, R> {
    R apply(T r, long v);
}
