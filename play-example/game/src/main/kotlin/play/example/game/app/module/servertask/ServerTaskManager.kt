package play.example.game.app.module.servertask

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.example.game.app.module.servertask.res.ServerTaskResourceSet
import play.example.game.app.module.task.TaskEventBus
import play.example.game.app.module.task.event.TaskEvent

class ServerTaskManager(
  ctx: ActorContext<Command>,
  private val taskService: ServerTaskService,
  taskEventBus: TaskEventBus
) : AbstractTypedActor<ServerTaskManager.Command>(ctx) {

  interface Command
  object ResourceReloaded : Command
  private class ServerTaskEvent(val event: TaskEvent) : Command

  init {
    taskEventBus.subscribe(context.messageAdapter(TaskEvent::class.java, ::ServerTaskEvent)) { event ->
      ServerTaskResourceSet.extension().targetTypes.contains(event.type)
    }
  }

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onResourceReload)
      .accept(::onTaskEvent)
      .build()
  }

  private fun onTaskEvent(serverTaskEvent: ServerTaskEvent) {
    taskService.onEvent(serverTaskEvent.event)
  }

  private fun onResourceReload(cmd: ResourceReloaded) {
    taskService.checkNewTask()
    taskService.checkShouldFinished()
  }
}
