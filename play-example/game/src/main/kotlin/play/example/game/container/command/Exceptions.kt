package play.example.game.container.command

import play.util.exception.NoStackTraceException

class CommandArgMissingException(message: String) : NoStackTraceException(message)

class CommandIllegalArgException(message: String) : NoStackTraceException(message)
