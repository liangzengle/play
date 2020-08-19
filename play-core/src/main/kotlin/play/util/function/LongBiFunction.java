package play.util.function;

@FunctionalInterface
public interface LongBiFunction<R> {
    R apply(long arg1, long arg2);
}
