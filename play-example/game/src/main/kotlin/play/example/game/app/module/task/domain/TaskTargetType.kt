package play.example.game.app.module.task.domain

import play.example.game.app.module.task.target.TaskTarget

/**
 * 任务目标类型
 * @property taskTargetClass 任务目标的Class
 * @author LiangZengle
 */
interface TaskTargetType {
  val taskTargetClass: Class<out TaskTarget>
}
