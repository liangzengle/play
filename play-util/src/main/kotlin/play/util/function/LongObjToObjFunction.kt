package play.util.function

fun interface LongObjToObjFunction<T, R> {
  fun apply(key: Long, value: T): R
}
