package play.example.game.container.net

import akka.actor.typed.ActorRef
import io.netty.channel.Channel
import io.netty.util.AttributeMap
import io.netty.util.concurrent.ScheduledFuture
import org.jctools.maps.NonBlockingHashMapLong
import play.mvc.Response
import play.util.collection.LongIterable
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.LongStream

class Session(private val ch: Channel, val actorRef: ActorRef<SessionActor.Command>, flushIntervalMillis: Int) :
  AttributeMap by ch {

  val remoteIp = when (val address = ch.remoteAddress()) {
    is InetSocketAddress -> address.hostString
    else -> address.toString()
  }

  val remotePort = when (val address = ch.remoteAddress()) {
    is InetSocketAddress -> address.port
    else -> 0
  }

  private val writer: SessionWriter

  private var flushSchedule: ScheduledFuture<*>? = null

  init {
    writer = if (flushIntervalMillis > 0) SessionWriter.WriteNoFlush(ch) else SessionWriter.WriteFlush(ch)
    if (writer is SessionWriter.WriteNoFlush) {
      val future = ch.eventLoop()
        .scheduleWithFixedDelay(
          { writer.flush() },
          flushIntervalMillis.toLong(),
          flushIntervalMillis.toLong(),
          TimeUnit.MILLISECONDS
        )
      this.flushSchedule = future
      ch.closeFuture().addListener {
        future.cancel(false)
      }
    }
  }

  fun write(msg: Response) {
    writer.write(msg)
  }

  fun tellActor(msg: SessionActor.Command) {
    actorRef.tell(msg)
  }

  override fun toString(): String {
    return ch.toString()
  }

  companion object {
    @JvmStatic
    private val identifiedSessions = NonBlockingHashMapLong<Session>()

    @JvmStatic
    fun bind(id: Long, session: Session): Session? {
      return identifiedSessions.put(id, session)
    }

    @JvmStatic
    fun unbind(id: Long, session: Session) {
      identifiedSessions.remove(id, session as Any)
    }

    @JvmStatic
    fun write(id: Long, msg: Response) {
      identifiedSessions[id]?.write(msg)
    }

    @JvmStatic
    fun writeAll(ids: Iterable<Long>, msg: Response) {
      for (id in ids) {
        write(id, msg)
      }
    }

    @JvmStatic
    fun writeAll(ids: LongIterable, msg: Response) {
      for (id in ids) {
        write(id, msg)
      }
    }

    @JvmStatic
    fun writeAll(ids: PrimitiveIterator.OfLong, msg: Response) {
      while (ids.hasNext()) {
        write(ids.nextLong(), msg)
      }
    }

    @JvmStatic
    fun writeAll(ids: LongStream, msg: Response) {
      writeAll(ids.iterator(), msg)
    }
  }
}

/**
 * 发送消息
 * @author LiangZengle
 */
private sealed class SessionWriter(protected val ch: Channel) {

  abstract fun write(msg: Any)

  abstract fun flush(): Boolean

  class WriteNoFlush(ch: Channel) : SessionWriter(ch) {
    @Volatile
    private var empty: Boolean = true
    override fun write(msg: Any) {
      ch.write(msg, ch.voidPromise())
      empty = false
    }

    override fun flush(): Boolean {
      if (empty) {
        return false
      }
      empty = true
      ch.flush()
      return true
    }
  }

  class WriteFlush(ch: Channel) : SessionWriter(ch) {
    override fun write(msg: Any) {
      ch.writeAndFlush(msg, ch.voidPromise())
    }

    override fun flush(): Boolean {
      ch.flush()
      return true
    }
  }
}
