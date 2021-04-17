package play.util.primitive

fun Boolean.toByte(): Byte = if (this) 1 else 0

fun Byte.toBoolean(): Boolean = this != 0.toByte()

fun Int.toBoolean(): Boolean = this != 0
