package play.example.game.container.gs.logging

import akka.japi.function.Function
import play.util.unsafeCast

class ActorMdc(
  val staticMdc: Map<String, String>,
  private val mdcPerMessage: Function<Any, Map<String, String>>
) {
  constructor(staticMdc: Map<String, String>) : this(staticMdc, noMdcPerMessage)

  companion object {
    private val noMdcPerMessage = Function<Any, Map<String, String>> { emptyMap() }
  }

  fun <T> mdcPerMessage(): Function<T, Map<String, String>> = mdcPerMessage.unsafeCast()
}
