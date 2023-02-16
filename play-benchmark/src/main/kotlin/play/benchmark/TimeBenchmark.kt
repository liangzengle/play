package play.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 *
 * @author LiangZengle
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class TimeBenchmark {
  private var units = listOf("ns", "us", "ms", "s", "m", "h", "d", "NS", "US", "MS", "S", "M", "H", "D")

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  @Benchmark
  fun parseNew(bh: Blackhole) {
    for (i in units.indices) {
      val unit = units[i]
      val len = unit.length
      val result = if (len == 1) {
        when (unit[0].lowercaseChar()) {
          's' -> ChronoUnit.SECONDS
          'm' -> ChronoUnit.MINUTES
          'h' -> ChronoUnit.HOURS
          'd' -> ChronoUnit.DAYS
          else -> throw IllegalArgumentException("Unknown unit: $unit")
        }
      } else if (len == 2 && unit[1].lowercaseChar() == 's') {
        when (unit[0].lowercaseChar()) {
          'm' -> ChronoUnit.MILLIS
          'n' -> ChronoUnit.NANOS
          'u' -> ChronoUnit.MICROS
          else -> throw IllegalArgumentException("Unknown unit: $unit")
        }
      } else {
        throw IllegalArgumentException("Unknown unit: $unit")
      }
      bh.consume(result)
    }
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  @Benchmark
  fun parseOld(bh: Blackhole) {
    for (i in units.indices) {
      val result = when (units[0].lowercase()) {
        "ms" -> ChronoUnit.MILLIS
        "s" -> ChronoUnit.SECONDS
        "m" -> ChronoUnit.MINUTES
        "h" -> ChronoUnit.HOURS
        "d" -> ChronoUnit.DAYS
        "ns" -> ChronoUnit.NANOS
        "us" -> ChronoUnit.MICROS
        else -> throw IllegalArgumentException("Unknown unit: ${units[0]}")
      }
      bh.consume(result)
    }
  }
}
