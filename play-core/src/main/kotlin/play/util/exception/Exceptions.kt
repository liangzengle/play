package play.util.exception

fun Throwable.isFatal(): Boolean = when (this) {
  is VirtualMachineError -> true
  is ThreadDeath -> true
  is InterruptedException -> true
  is LinkageError -> true
  else -> false
}

fun Throwable.isNotFatal(): Boolean = !isFatal()
