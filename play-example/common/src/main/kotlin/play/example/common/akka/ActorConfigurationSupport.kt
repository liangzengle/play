package play.example.common.akka

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import play.util.concurrent.Promise
import java.time.Duration

/**
 *
 * @author LiangZengle
 */
interface ActorConfigurationSupport {

  fun <T> spawn(actorSystem: ActorSystem<GuardianBehavior.Command>, behavior: Behavior<T>, name: String): ActorRef<T> {
    val promise = Promise.make<ActorRef<T>>()
    val cmd = GuardianBehavior.Spawn(behavior, name, promise)
    actorSystem.tell(cmd)
    return promise.future.get(Duration.ofSeconds(3))
  }
}
