@file:JvmName("Cleaners")

package play.util

import java.lang.ref.Cleaner

object Cleaners {

  private val cleaner: Cleaner = Cleaner.create(Thread.ofPlatform().name("play-cleaner").factory())

  /**
   * @see [Cleaner.register]
   */
  fun register(obj: Any, action: Runnable): Cleaner.Cleanable {
    return cleaner.register(obj, action)
  }
}
