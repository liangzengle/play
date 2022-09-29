package play.util.time

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 *
 *
 * @author LiangZengle
 */
internal class OffsetDayTest {

  @Test
  fun isSameDay0() {
    val offsetDay = OffsetDay(LocalTime.of(0, 0, 0))
    assertTrue {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 0, 0, 0),
        LocalDateTime.of(2022, 1, 1, 0, 0, 0)
      )
    }
    assertTrue {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 0, 0, 0),
        LocalDateTime.of(2022, 1, 1, 23, 59, 59)
      )
    }
    assertFalse {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 0, 0, 0),
        LocalDateTime.of(2022, 1, 2, 0, 0, 0)
      )
    }
    assertFalse {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 0, 0, 0),
        LocalDateTime.of(2022, 1, 3, 0, 0, 0)
      )
    }
  }

  @Test
  fun isSameDay5() {
    val offsetDay = OffsetDay(LocalTime.of(5, 0, 0))
    assertTrue {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 5, 0, 0),
        LocalDateTime.of(2022, 1, 1, 6, 0, 0)
      )
    }
    assertFalse {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 4, 0, 0),
        LocalDateTime.of(2022, 1, 1, 5, 0, 0)
      )
    }
    assertTrue {
      offsetDay.isSameDay(
        LocalDateTime.of(2022, 1, 1, 5, 0, 0),
        LocalDateTime.of(2022, 1, 2, 4, 0, 0)
      )
    }
  }

  @Test
  fun asStartOfDay0() {
    val offsetDay = OffsetDay(LocalTime.of(0, 0, 0))
    assertEquals(
      LocalDateTime.of(2022,1,1, 0,0,0),
      offsetDay.atStartOfDay(LocalDateTime.of(2022, 1, 1, 0, 0, 0))
    )
    assertEquals(
      LocalDate.of(2022,1,1),
      offsetDay.atStartOfDay(LocalDateTime.of(2022, 1, 1, 0, 0, 0)).toLocalDate()
    )
    assertEquals(
      LocalDate.of(2022,1,1),
      offsetDay.atStartOfDay(LocalDateTime.of(2022, 1, 1, 23, 59, 59)).toLocalDate()
    )
  }

  @Test
  fun asStartOfDay5() {
    val offsetDay = OffsetDay(LocalTime.of(5, 0, 0))
    assertEquals(
      LocalDateTime.of(2021,12,31,5,0,0),
      offsetDay.atStartOfDay(LocalDateTime.of(2022, 1, 1, 0, 0, 0))
    )
    assertEquals(
      LocalDate.of(2022,1,1),
      offsetDay.atStartOfDay(LocalDateTime.of(2022, 1, 1, 23, 59, 59)).toLocalDate()
    )
  }
}
