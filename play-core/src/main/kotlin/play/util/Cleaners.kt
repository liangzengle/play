@file:JvmName("Cleaners")

package play.util

import play.util.concurrent.NamedThreadFactory
import java.lang.ref.Cleaner

object Cleaners {

  private val cleaner: Cleaner = Cleaner.create(NamedThreadFactory.newBuilder("play-cleaner").build())

  /**
   * @see [Cleaner.register]
   */
  fun register(obj: Any, action: Runnable): Cleaner.Cleanable {
    return cleaner.register(obj, action)
  }
}
