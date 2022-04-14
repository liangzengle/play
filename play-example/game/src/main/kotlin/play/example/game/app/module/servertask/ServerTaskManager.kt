package play.example.game.app.module.servertask

import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import play.akka.AbstractTypedActor
import play.example.game.app.module.servertask.event.ServerTaskEvent

class ServerTaskManager(
  ctx: ActorContext<Command>,
  private val taskService: ServerTaskService
) : AbstractTypedActor<ServerTaskManager.Command>(ctx) {

  interface Command
  object ResourceReloaded : Command

  override fun createReceive(): Receive<Command> {
    return newReceiveBuilder()
      .accept(::onResourceReload)
      .accept(::onTaskEvent)
      .build()
  }

  private fun onTaskEvent(event: ServerTaskEvent) {
    taskService.onEvent(event)
  }

  private fun onResourceReload(cmd: ResourceReloaded) {
    taskService.checkNewTask()
    taskService.checkShouldFinished()
  }
}
