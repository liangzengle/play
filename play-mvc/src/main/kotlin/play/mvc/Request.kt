package play.mvc

data class Request(val header: Header, @JvmField val body: RequestBody) {
  fun msgId() = header.msgId.toInt()
}
