package play.util.function

fun interface LongToObjBiFunction<R> {
  fun apply(key: Long, value: Long): R
}
