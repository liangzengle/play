package play.scala

import scala.Option
import java.util.*


fun <T : Any> scalaOption(value: T?): Option<T?> = Option.apply(value)

fun <T : Any> Option<T>.toJava(): Optional<T> = if (isDefined) Optional.of(get()) else Optional.empty()

fun Option<Int>.toOptionalInt(): OptionalInt = if (isDefined) OptionalInt.of(get()) else OptionalInt.empty()

fun Option<Long>.toOptionalLong(): OptionalLong = if (isDefined) OptionalLong.of(get()) else OptionalLong.empty()

fun Option<Double>.toOptionalDouble(): OptionalDouble =
  if (isDefined) OptionalDouble.of(get()) else OptionalDouble.empty()
