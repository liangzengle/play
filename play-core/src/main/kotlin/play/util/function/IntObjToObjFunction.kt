package play.util.function

fun interface IntObjToObjFunction<T, R> {
  operator fun invoke(key: Int, value: T): R
}
