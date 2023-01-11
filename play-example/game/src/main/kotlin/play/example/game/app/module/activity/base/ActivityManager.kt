package play.example.game.app.module.activity.base

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.eclipse.collections.api.factory.primitive.IntObjectMaps
import play.akka.AbstractTypedActor
import play.akka.logging.ActorMDC
import play.event.EventBus
import play.example.game.app.module.activity.base.res.ActivityResource
import play.example.game.app.module.activity.base.res.ActivityResourceSet
import play.example.game.app.module.server.event.ServerOpenEvent
import play.spring.SingletonBeanContext
import play.util.classOf
import play.util.concurrent.PlayFuture
import play.util.concurrent.Promise
import play.util.unsafeCast
import java.util.*

/**
 *
 * @author LiangZengle
 */
class ActivityManager(
  ctx: ActorContext<Command>,
  private val mdc: ActorMDC,
  private val beanContext: SingletonBeanContext
) : AbstractTypedActor<ActivityManager.Command>(ctx) {

  private val activityActors = IntObjectMaps.mutable.empty<ActorRef<ActivityActor.Command>>()

  init {
    beanContext.getBean<EventBus>().subscribe0(ServerOpenEvent::class.java) {
      self send Init
    }
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::init)
      .build()
  }

  private fun initialized() = newBehaviorBuilder()
    .accept<ResourceReloaded>(::onResourceReloaded)
    .accept(::onStopped)
    .accept(::startChildren)
    .accept(::closeChildren)
    .accept(::onActivityInitialized)
    .build()

  private fun init(cmd: Init): Behavior<Command> {
    for (resource in ActivityResourceSet.list()) {
      val ref = createActivityActor(resource)
      if (resource.parentId == 0) {
        ref.tell(ActivityActor.Init)
      }
    }
    return initialized()
  }

  private fun onResourceReloaded() {
    val activityResources = ActivityResourceSet.list()
    for (resource in activityResources) {
      if (!activityActors.containsKey(resource.id)) {
        val ref = createActivityActor(resource)
        if (resource.parentId == 0) {
          ref.tell(ActivityActor.Init)
        }
      }
    }
    for (activityResource in activityResources) {
      activityActors.get(activityResource.id).tell(ActivityActor.CheckResourceReload)
    }
  }

  private fun createActivityActor(resource: ActivityResource): ActorRef<ActivityActor.Command> {
    val ref = spawn(
      resource.id.toString(),
      classOf(),
      mdc,
      ActivityActor.create(
        resource.id,
        self,
        beanContext.getBean(),
        beanContext.getBean(),
        beanContext.getBean(),
        beanContext.getImpl(classOf(), resource.type),
        beanContext.getBean(),
        beanContext.getBean(),
        beanContext.getBean(),
        beanContext.getBean()
      )
    )
    activityActors.put(resource.id, ref)
    context.watchWith(ref, Terminated(resource.id))
    return ref
  }

  private fun onActivityInitialized(cmd: ActivityInitialized) {
    ActivityResourceSet.extension().getChildActivityIds(cmd.id).forEach { id ->
      activityActors.get(id)?.tell(ActivityActor.Init)
    }
  }

  private fun onStopped(cmd: Terminated) {
    activityActors.remove(cmd.id)
  }

  private fun startChildren(cmd: StartChildren) {
    ActivityResourceSet.extension().getChildActivityIds(cmd.parentId).forEach { id ->
      activityActors.get(id)?.tell(ActivityActor.Init)
    }
  }

  private fun closeChildren(cmd: CloseChildren) {
    val futures = ActivityResourceSet.extension().getChildActivityIds(cmd.parentId)
      .asLazy()
      .collect { id -> activityActors.get(id) }
      .select(Objects::nonNull)
      .collect { ref ->
        val promise = Promise.make<Unit>()
        ref.tell(ActivityActor.ForceClose(promise))
        promise.future
      }.toList()
    cmd.promise.completeWith(PlayFuture.allOf(futures).unsafeCast())
  }

  companion object {
    fun create(mdc: ActorMDC, beanContext: SingletonBeanContext): Behavior<Command> {
      return Behaviors.setup { ctx ->
        ActivityManager(ctx, mdc, beanContext)
      }
    }
  }

  interface Command

   object Init : Command

  object ResourceReloaded : Command

  private data class Terminated(val id: Int) : Command

  data class ActivityInitialized(val id: Int) : Command

  class StartChildren(val parentId: Int) : Command
  class CloseChildren(val parentId: Int, val promise: Promise<Unit>) : Command
}
