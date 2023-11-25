package play.util.function

fun interface ObjLongToObjFunction<T, R> {
  fun apply(k: T, v: Long): R
}
