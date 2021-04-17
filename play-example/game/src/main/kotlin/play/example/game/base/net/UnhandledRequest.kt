package play.example.game.base.net

import akka.actor.typed.ActorRef
import play.mvc.Request

/**
 * @author LiangZengle
 */
class UnhandledRequest(val request: Request, val session: ActorRef<SessionActor.Command>)
