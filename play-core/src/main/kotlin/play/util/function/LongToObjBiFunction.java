package play.util.function;

@FunctionalInterface
public interface LongToObjBiFunction<T, R> {
    R apply(long arg, T t);
}
