package play.util.function

fun interface LongLongToObjFunction<R> {
  fun apply(key: Long, value: Long): R
}
