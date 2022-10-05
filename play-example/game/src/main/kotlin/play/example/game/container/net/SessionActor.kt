package play.example.game.container.net

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import play.akka.AbstractTypedActor
import play.mvc.Request
import play.mvc.Response
import play.util.logging.getLogger

class SessionActor(
  context: ActorContext<Command>,
  private val ch: Channel,
  private val unhandledRequestReceivers: List<ActorRef<UnhandledRequest>>,
  flushIntervalMillis: Int
) : AbstractTypedActor<SessionActor.Command>(context) {

  private val session: Session

  private var id = 0L

  private var subscriber: ActorRef<Request>? = null

  @Volatile
  private var terminated = false

  init {
    ch.pipeline().addLast("session", ChannelHandler())
    session = Session(ch, context.self, flushIntervalMillis)
    ch.config().isAutoRead = true
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder().accept(::close).accept(::notifyAndClose).accept(::bind).accept(::subscribe)
      .accept(::write).acceptSignal<PostStop>(::postStop).build()
  }

  private fun postStop() {
    Session.unbind(id, session)
    if (ch.isActive) {
      ch.close()
    }
  }

  private fun write(cmd: Write): Behavior<Command> {
    if (!ch.isActive) {
      return stoppedBehavior()
    }
    session.write(cmd.msg)
    return sameBehavior()
  }

  private fun notifyAndClose(cmd: NotifyAndClose): Behavior<Command> {
    session.write(cmd.msg)
    return close(Close(cmd.reason))
  }

  private fun close(cmd: Close): Behavior<Command> {
    ch.close()
    if (cmd.cause != null) {
      logger.error(cmd.cause) { "Session Closed: $ch ${cmd.reason}" }
    } else {
      logger.info { "Session Closed: $ch ${cmd.reason}" }
    }
    return stoppedBehavior()
  }

  private fun forceClose(reason: String, cause: Throwable? = null) {
    if (!terminated) {
      terminated = true
      ch.close()
      self.tell(Close(reason, cause))
    }
  }

  private fun bind(cmd: BindId) {
    if (this.id != 0L) {
      logger.error { "Session already bound to id: $id" }
      return
    }
    this.id = cmd.id
    val prev = Session.bind(cmd.id, session)
    prev?.tellActor(Close("Session replaced: id=$id, prev=$prev, current=$session"))
  }

  private fun subscribe(cmd: Subscribe) {
    subscriber = cmd.subscriber
  }

  private inner class ChannelHandler : ChannelInboundHandlerAdapter() {
    override fun channelInactive(ctx: ChannelHandlerContext) {
      forceClose("Channel Inactive")
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
      if (msg !is Request) {
        ctx.fireChannelRead(msg)
        return
      }
      val receiver = subscriber
      if (receiver != null) {
        receiver.tell(msg)
      } else {
        val unhandledRequest = UnhandledRequest(msg, session)
        for (index in unhandledRequestReceivers.indices) {
          unhandledRequestReceivers[index].tell(unhandledRequest)
        }
      }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
      when (evt) {
        is IdleStateEvent -> {
          if (evt.state() == IdleState.READER_IDLE) {
            forceClose("Read Idle Timeout")
          }
        }

        else -> logger.info { "userEventTriggered: $evt" }
      }
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
      if (!ctx.channel().isWritable) {
        forceClose("Channel become unWritable")
      } else {
        super.channelWritabilityChanged(ctx)
      }
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
      forceClose("Exception Occurred", cause)
    }
  }

  companion object {
    private val logger = getLogger()

    fun create(ch: Channel, unhandledRequestReceivers: List<ActorRef<UnhandledRequest>>): Behavior<Command> =
      Behaviors.setup { ctx ->
        SessionActor(ctx, ch, unhandledRequestReceivers, 0)
      }
  }

  interface Command
  data class Close(val reason: String, val cause: Throwable? = null) : Command
  data class NotifyAndClose(val reason: String, val msg: Response) : Command
  data class BindId(val id: Long) : Command
  data class Subscribe(val subscriber: ActorRef<Request>) : Command
  data class Write(val msg: Response) : Command
}
