package play.example.common.net

import io.netty.channel.Channel


/**
 *
 * @author LiangZengle
 */
sealed class SessionWriter(protected val ch: Channel) {

  abstract fun write(msg: Any)

  abstract fun flush()

  class WriteNoFlush(ch: Channel) : SessionWriter(ch) {
    @Volatile
    private var empty: Boolean = true
    override fun write(msg: Any) {
      ch.write(msg, ch.voidPromise())
      empty = false
    }

    override fun flush() {
      if (!empty) {
        ch.flush()
        empty = true
      }
    }
  }

  class WriteFlush(ch: Channel) : SessionWriter(ch) {
    override fun write(msg: Any) {
      ch.writeAndFlush(msg, ch.voidPromise())
    }

    override fun flush() {
      ch.flush()
    }
  }
}
