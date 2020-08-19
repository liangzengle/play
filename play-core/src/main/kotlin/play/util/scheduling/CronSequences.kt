@file:Suppress("NOTHING_TO_INLINE")

package play.util.scheduling

import play.util.time.toDate
import play.util.time.toLocalDateTime
import java.time.LocalDateTime

inline fun CronSequenceGenerator.nextFireTime(from: LocalDateTime): LocalDateTime {
  return next(from.toDate()).toLocalDateTime()
}

inline fun CronSequenceGenerator.prevFireTime(from: LocalDateTime): LocalDateTime {
  return prev(from.toDate())?.toLocalDateTime()
    ?: throw IllegalStateException("can not find prevFireTime of ${this.expression} from [$from]")
}
