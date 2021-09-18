package play.example.game.container.login

import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import org.eclipse.collections.api.set.primitive.IntSet
import play.akka.AbstractTypedActor
import play.example.game.app.module.account.controller.AccountModule
import play.example.game.app.module.account.message.LoginParams
import play.example.game.container.net.Session
import play.example.game.container.net.SessionManager
import play.example.game.container.net.UnhandledRequest
import play.mvc.MessageCodec
import play.mvc.Request

/**
 *
 * @author LiangZengle
 */
class LoginDispatcherActor(ctx: ActorContext<Command>, sessionManager: ActorRef<SessionManager.Command>) :
  AbstractTypedActor<LoginDispatcherActor.Command>(ctx) {
  interface Command
  class UnhandledLoginRequest(val request: Request, val session: Session) : Command
  class RegisterLoginReceiver(val serverIds: IntSet, val receiver: ActorRef<UnhandledLoginRequest>) : Command

  private val receivers = ArrayList<RegisterLoginReceiver>(1)

  init {
    val receiver =
      context.messageAdapter(UnhandledRequest::class.java) { UnhandledLoginRequest(it.request, it.session) }
    sessionManager.tell(SessionManager.RegisterUnhandledRequestReceiver(receiver))
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::handle)
      .accept(::register)
      .build()
  }

  private fun register(cmd: RegisterLoginReceiver) {
    receivers.add(cmd)
  }

  private fun handle(req: UnhandledLoginRequest) {
    when (req.request.msgId()) {
      AccountModule.login -> dispatch(req)
      else -> context.log.warn("Unhandled request: {}", req.request)
    }
  }

  private fun dispatch(req: UnhandledLoginRequest) {
    val requestBody = req.request.body
    val params = MessageCodec.decode<LoginParams>(requestBody.bytes)
    val serverId = params.serverId
    for (i in receivers.indices) {
      val cmd = receivers[i]
      val receiver = cmd.receiver
      val serverIds = cmd.serverIds
      if (serverIds.contains(serverId)) {
        receiver.tell(req)
        return
      }
    }
    context.log.info("No receiver for login message: {}", params)
  }
}
