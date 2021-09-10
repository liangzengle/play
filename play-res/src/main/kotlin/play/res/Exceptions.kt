package play.res

import play.util.exception.NoStackTraceException

class ResourceNotFoundException(path: String) : NoStackTraceException(path)

class InvalidResourceException(msg: String) : NoStackTraceException(msg) {

  constructor(messages: Iterable<String>) : this(messages.joinToString("\n", "\n", ""))

  constructor(resourceClass: Class<*>, message: String) : this("[${resourceClass.simpleName}]$message")
}

class IllegalConcreteTypeException(type: Class<*>) :
  RuntimeException("Should use Abstract Super Type instead of Concrete Type: $type")
