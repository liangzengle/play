package play.util.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Test
import play.util.time.Time
import java.time.Duration
import java.time.format.DateTimeParseException

/**
 *
 * @author LiangZengle
 */
internal class TimeTest {

  @Test
  fun parseDuration() {
    assertEquals(Duration.ofMillis(100), Time.parseDuration("100"))
    assertEquals(Duration.ofMillis(-100), Time.parseDuration("-100"))
    assertEquals(Duration.ofMinutes(1), Time.parseDuration("1m"))
    assertEquals(Duration.ofMinutes(-1), Time.parseDuration("-1m"))
    assertEquals(Duration.ofMillis(1010), Time.parseDuration("1s10ms"))
    assertEquals(Duration.ofMillis(990), Time.parseDuration("1s-10ms"))
    assertEquals(Duration.ofMillis(1010), Time.parseDuration("1s10"))
    assertEquals(Duration.ofMillis(990), Time.parseDuration("1s-10"))
    assertEquals(
      Duration.ofDays(1).plus(Duration.ofHours(1).plus(Duration.ofMinutes(1)).plus(Duration.ofSeconds(1)))
        .plus(Duration.ofMillis(1)),
      Time.parseDuration("1d1h1m1s1ms")
    )

    assertEquals(Duration.ofDays(1), Time.parseDuration("P1D"))
    assertEquals(Duration.ofDays(1).plus(Duration.ofHours(1)), Time.parseDuration("P1DT1H"))
    assertEquals(Duration.ofHours(1), Time.parseDuration("PT1H"))

    assertThrowsExactly(DateTimeParseException::class.java) {
      Time.parseDuration("s")
    }
    assertThrowsExactly(DateTimeParseException::class.java) {
      Time.parseDuration("1hs")
    }
    assertThrowsExactly(DateTimeParseException::class.java) {
      Time.parseDuration("1q")
    }
  }
}
