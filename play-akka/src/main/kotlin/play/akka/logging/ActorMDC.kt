package play.akka.logging

import akka.japi.function.Function
import play.util.unsafeCast

class ActorMDC(
  val staticMdc: Map<String, String>,
  private val mdcPerMessage: Function<Any, Map<String, String>>
) {
  constructor(staticMDC: Map<String, String>) : this(staticMDC, noMDCPerMessage)

  companion object {
    private val noMDCPerMessage = Function<Any, Map<String, String>> { emptyMap() }
  }

  fun <T> mdcPerMessage(): Function<T, Map<String, String>> = mdcPerMessage.unsafeCast()
}
