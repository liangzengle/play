package play.util.function

fun interface IntToObjFunction<R> {
  operator fun invoke(value: Int): R
}
