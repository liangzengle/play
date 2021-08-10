package play.example.game.app.module.task.config

import play.example.game.app.module.reward.model.Reward
import play.example.game.app.module.task.target.TaskTarget
import play.res.AbstractResource
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * 任务配置基类
 *
 * @author LiangZengle
 */
abstract class AbstractTaskResource(
  @JvmField @field:NotEmpty val targets: List<TaskTarget> = emptyList(),
  @JvmField @field:Valid val rewards: List<Reward> = emptyList()
) : AbstractResource()
