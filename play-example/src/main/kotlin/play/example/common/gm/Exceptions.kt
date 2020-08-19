package play.example.common.gm

import play.util.exception.NoStackTraceException

class GmCommandArgMissingException(message: String) : NoStackTraceException(message)

class GmCommandIllegalArgException(message: String) : NoStackTraceException(message)
