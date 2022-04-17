package play.akka.scheduling

import akka.actor.Cancellable
import play.scheduling.Canceller

object AkkaCancellableCanceller : Canceller<Cancellable> {
  override fun cancel(target: Cancellable): Boolean {
    return target.cancel()
  }
}
