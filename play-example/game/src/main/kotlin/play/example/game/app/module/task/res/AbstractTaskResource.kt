package play.example.game.app.module.task.res

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import play.example.game.app.module.reward.model.RewardList
import play.example.game.app.module.task.target.TaskTarget
import play.res.AbstractResource

/**
 * 任务配置基类
 *
 * @author LiangZengle
 */
abstract class AbstractTaskResource(
  @JvmField @field:NotEmpty val targets: List<TaskTarget> = emptyList(),
  @JvmField @field:Valid val rewards: RewardList = RewardList.Empty
) : AbstractResource()
