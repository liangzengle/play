package play.config

import play.util.exception.NoStackTraceException

class ResourceNotFoundException(path: String) : NoStackTraceException(path)

class InvalidConfigException(msg: String) : NoStackTraceException(msg) {

  constructor(messages: Iterable<String>) : this(messages.joinToString("\n", "\n", ""))

  constructor(configClass: Class<*>, message: String) : this("[${configClass.simpleName}]$message")
}
