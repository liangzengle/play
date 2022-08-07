package play.example.game.container.login

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.set.primitive.IntSet
import play.akka.AbstractTypedActor
import play.codec.MessageCodec
import play.example.game.app.module.account.AccountModule
import play.example.game.container.net.Session
import play.example.game.container.net.SessionManager
import play.example.game.container.net.UnhandledRequest
import play.example.module.login.message.LoginParams
import play.mvc.Request

/**
 * 登录消息按服id分发
 * @author LiangZengle
 */
class LoginDispatcherActor(ctx: ActorContext<Command>, sessionManager: ActorRef<SessionManager.Command>) :
  AbstractTypedActor<LoginDispatcherActor.Command>(ctx) {
  interface Command
  class UnhandledLoginRequest(@JvmField val request: Request, @JvmField val session: Session) : Command
  class RegisterLoginReceiver(
    @JvmField val serverIds: IntSet, @JvmField val receiver: ActorRef<UnhandledLoginRequest>
  ) : Command

  private class UnregisterLoginReceiver(val receiver: ActorRef<UnhandledLoginRequest>) : Command

  private val loginParamCodec = MessageCodec.newCodec(LoginParams::class)

  private val receivers = Lists.mutable.ofInitialCapacity<RegisterLoginReceiver>(1)

  init {
    val receiver =
      context.messageAdapter(UnhandledRequest::class.java) { UnhandledLoginRequest(it.request, it.session) }
    sessionManager.tell(SessionManager.RegisterUnhandledRequestReceiver(receiver))
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder().accept(::handle).accept(::register).accept(::unregister).build()
  }

  private fun register(cmd: RegisterLoginReceiver) {
    context.watchWith(cmd.receiver, UnregisterLoginReceiver(cmd.receiver))
    receivers.add(cmd)
  }

  private fun unregister(cmd: UnregisterLoginReceiver) {
    receivers.removeIf(Predicate { it.receiver === cmd.receiver })
  }

  private fun handle(req: UnhandledLoginRequest) {
    when (req.request.msgId()) {
      AccountModule.login -> dispatch(req)
      else -> log.warn("Unhandled request: {}", req.request)
    }
  }

  private fun dispatch(req: UnhandledLoginRequest) {
    try {
      val requestBody = req.request.body
      val params = loginParamCodec.decode(requestBody.getPayload())
      val serverId = params.serverId
      for (i in receivers.indices) {
        val cmd = receivers[i]
        if (cmd.serverIds.contains(serverId)) {
          cmd.receiver.tell(req)
          return
        }
      }
      log.info("No receiver for login message: {}", params)
    } catch (e: Exception) {
      log.error(e.message, e)
    }
  }
}
