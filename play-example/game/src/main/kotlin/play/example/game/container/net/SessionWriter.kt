package play.example.game.container.net

import io.netty.channel.Channel
import play.util.collection.LongIterable
import java.util.*
import java.util.stream.LongStream

/**
 * 发送消息
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
        empty = true
        ch.flush()
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

  companion object {
    @JvmStatic
    fun write(id: Long, msg: Any) = SessionActor.write(id, msg)

    @JvmStatic
    fun writeAll(ids: Iterable<Long>, msg: Any) = SessionActor.writeAll(ids, msg)

    @JvmStatic
    fun writeAll(ids: LongIterable, msg: Any) = SessionActor.writeAll(ids, msg)

    @JvmStatic
    fun writeAll(ids: PrimitiveIterator.OfLong, msg: Any) = SessionActor.writeAll(ids, msg)

    @JvmStatic
    fun writeAll(ids: LongStream, msg: Any) = SessionActor.writeAll(ids, msg)
  }
}
