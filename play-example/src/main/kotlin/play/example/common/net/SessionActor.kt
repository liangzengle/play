package play.example.common.net

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.jctools.maps.NonBlockingHashMapLong
import play.akka.AbstractTypedActor
import play.akka.sameBehavior
import play.akka.stoppedBehavior
import play.getLogger
import play.mvc.Request
import play.mvc.Response
import java.time.Duration
import java.util.*

class SessionActor(
  context: ActorContext<Command>,
  private val ch: Channel,
  timer: TimerScheduler<Command>,
  private val unhandledRequestReceivers: List<ActorRef<UnhandledRequest>>,
  flushIntervalMillis: Int
) : AbstractTypedActor<SessionActor.Command>(context) {

  private val writer: SessionWriter

  private var id = 0L

  private var subscriber: ActorRef<Request>? = null

  init {
    ch.pipeline().addLast("Forwarding", ForwardingChannelHandler())
    ch.config().isAutoRead = true
    if (flushIntervalMillis > 0) {
      writer = SessionWriter.WriteNoFlush(ch)
      timer.startTimerWithFixedDelay(Flush, Duration.ofMillis(flushIntervalMillis.toLong()))
    } else {
      writer = SessionWriter.WriteFlush(ch)
    }
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .onMessageEquals(Flush) { flush() }
      .accept(::close)
      .accept(::identify)
      .accept(::subscribe)
      .accept(::write)
      .onSignal<PostStop>(::postStop)
      .build()
  }

  private fun postStop() {
    sessions -= id
    if (ch.isActive) {
      ch.close()
    }
  }

  private fun flush(): Behavior<Command> {
    if (!ch.isActive) {
      return stoppedBehavior()
    }
    writer.flush()
    return sameBehavior()
  }

  private fun write(cmd: Write): Behavior<Command> {
    if (!ch.isActive) {
      return stoppedBehavior()
    }
    writer.write(cmd.msg)
    return sameBehavior()
  }

  private fun close(cmd: Close): Behavior<Command> {
    ch.close()
    if (cmd.cause != null) {
      logger.error(cmd.cause) { "Session Closed: ${cmd.reason}" }
    } else {
      logger.info { "Session Closed: ${cmd.reason}" }
    }
    return stoppedBehavior()
  }

  private fun forceClose(reason: String, cause: Throwable? = null) {
    ch.close()
    self.tell(Close(reason, cause))
  }

  private fun identify(cmd: Identify) {
    require(this.id == 0L) { "Session already identified as $id" }
    this.id = cmd.id
    val prev = sessions.putIfAbsent(cmd.id, writer)
    if (prev != null) {
      throw IllegalStateException("[${cmd.id}]已经绑定了Session[$ch]")
    }
  }

  private fun subscribe(cmd: Subscribe) {
    subscriber = cmd.subscriber
  }

  private inner class ForwardingChannelHandler : ChannelInboundHandlerAdapter() {
    override fun channelInactive(ctx: ChannelHandlerContext?) {
      forceClose("Channel Inactive")
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
      if (msg !is Request) {
        ctx.fireChannelRead(msg)
        return
      }
      val receiver = subscriber
      if (receiver == null) {
        val unhandledRequest = UnhandledRequest(msg, context.self)
        unhandledRequestReceivers.forEach { it.tell(unhandledRequest) }
      } else {
        receiver.tell(msg)
      }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
      when (evt) {
        is IdleStateEvent -> {
          if (evt.state() == IdleState.READER_IDLE) {
            forceClose("Read Idle Timeout")
          }
        }
        else -> println("userEventTriggered: $evt")
      }
    }

    @Suppress("DEPRECATION")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      forceClose("Exception Occurred", cause)
    }
  }

  companion object {
    private val logger = getLogger()

    private val sessions = NonBlockingHashMapLong<SessionWriter>()

    fun create(ch: Channel, unhandledRequestReceivers: List<ActorRef<UnhandledRequest>>): Behavior<Command> =
      Behaviors.setup { ctx ->
        Behaviors.withTimers { timer ->
          SessionActor(ctx, ch, timer, unhandledRequestReceivers, 50)
        }
      }

    fun write(id: Long, msg: Any) {
      sessions[id]?.write(msg)
    }

    fun writeAll(ids: Iterable<Long>, msg: Any) {
      for (id in ids) {
        sessions[id]?.write(msg)
      }
    }

    fun writeAll(ids: PrimitiveIterator.OfLong, msg: Any) {
      while (ids.hasNext()) {
        sessions[ids.nextLong()]?.write(msg)
      }
    }

    fun count(): Int = sessions.size
  }

  interface Command
  data class Close(val reason: String, val cause: Throwable? = null) : Command
  private object Flush : Command
  data class Identify(val id: Long) : Command
  data class Subscribe(val subscriber: ActorRef<Request>) : Command
  data class Write(val msg: Any) : Command
}

infix fun ActorRef<SessionActor.Command>.write(response: Response) = tell(SessionActor.Write(response))
