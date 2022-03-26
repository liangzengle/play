package play.example.game.app.module.servertask

import org.springframework.stereotype.Component
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.app.module.reward.RewardService
import play.example.game.app.module.reward.model.RewardResultSet
import play.example.game.app.module.servertask.domain.ServerTaskErrorCode
import play.example.game.app.module.servertask.domain.ServerTaskLogSource
import play.example.game.app.module.servertask.entity.PlayerServerTaskEntityCache
import play.util.control.Result2
import play.util.control.onOk

@Component
class PlayerServerTaskService(
  private val entityCache: PlayerServerTaskEntityCache,
  private val serverTaskService: ServerTaskService,
  private val rewardService: RewardService
) {

  fun receiveReward(self: Self, taskId: Int): Result2<RewardResultSet> {
    val entity = entityCache.getOrCreate(self.id)
    if (entity.rewardedTaskIds.contains(taskId)) {
      return ServerTaskErrorCode.TaskRewarded
    }
    if (!serverTaskService.isTaskFinished(taskId)) {
      return ServerTaskErrorCode.TaskNotFinished
    }
    val rewards = serverTaskService.getRewards(taskId)
    return rewardService.tryAndExecReward(self, rewards, ServerTaskLogSource.TaskReward)
      .onOk {
        entity.rewardedTaskIds.add(taskId)
      }
  }
}
