package play.util.function

fun interface ObjToLongFunction<T> {
  fun apply(t: T): Long
}
