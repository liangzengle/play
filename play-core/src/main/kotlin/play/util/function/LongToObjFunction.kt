package play.util.function

fun interface LongToObjFunction<R> {
  operator fun invoke(value: Long): R
}
