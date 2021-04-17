package play.example.game.module.task.config

import play.config.AbstractConfig
import play.example.game.module.reward.model.Reward
import play.example.game.module.task.target.TaskTarget
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

/**
 * 任务配置基类
 *
 * @author LiangZengle
 */
abstract class AbstractTaskConfig(
  @JvmField @field:NotEmpty val targets: List<TaskTarget> = emptyList(),
  @JvmField @field:Valid val rewards: List<Reward> = emptyList()
) : AbstractConfig()
