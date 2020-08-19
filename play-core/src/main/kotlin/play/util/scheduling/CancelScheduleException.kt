package play.util.scheduling

import play.util.exception.NoStackTraceException

object CancelScheduleException : NoStackTraceException("stop schedule")
