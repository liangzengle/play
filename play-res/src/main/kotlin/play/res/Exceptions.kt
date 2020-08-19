package play.res

import play.util.collection.mkString
import play.util.exception.NoStackTraceException

class SourceNotFoundException(path: String) : NoStackTraceException(path)

class InvalidResourceException(msg: String) : NoStackTraceException(msg) {

  constructor(messages: Iterable<String>) : this(messages.mkString('\n', '\n', '\n'))

  constructor(resourceClass: Class<*>, message: String) : this("[${resourceClass.simpleName}]$message")
}

internal class IllegalResourceFieldTypeException(msg: String) : NoStackTraceException(msg)
