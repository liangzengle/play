package play.example.game.container.command

import play.util.exception.NoStackTraceException

class CommandParamMissingException(message: String) : NoStackTraceException(message)

class CommandParamIllegalException(message: String) : NoStackTraceException(message)
