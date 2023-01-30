package play.scala

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

fun scalaDuration(length: Long, unit: TimeUnit): FiniteDuration = FiniteDuration.apply(length, unit)

fun FiniteDuration.toJava(): java.time.Duration = java.time.Duration.of(length(), unit().toChronoUnit())

fun java.time.Duration.toScala(): FiniteDuration = FiniteDuration.apply(this.toNanos(), TimeUnit.NANOSECONDS)
