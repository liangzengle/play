package play.example.game.app.module.task.event.adpater

import play.example.game.app.module.task.event.TaskEvent

/**
 *
 * @author LiangZengle
 */
interface TaskEventAdapter<FROM : TaskEvent, TO : TaskEvent> {

  fun convert(from: FROM): TO
}
