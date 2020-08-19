package play.util.primitive

fun Short.toByteArray(): ByteArray = byteArrayOf((this.toInt() shr 8).toByte(), this.toByte())

fun Short.toByteChecked(): Byte {
  if (this < Byte.MIN_VALUE || this > Byte.MAX_VALUE) throw ArithmeticException("overflow: $this")
  return toByte()
}

fun Short.toByteSaturated(): Byte {
  return when {
    this < Byte.MIN_VALUE -> Byte.MIN_VALUE
    this > Byte.MAX_VALUE -> Byte.MAX_VALUE
    else -> toByte()
  }
}
