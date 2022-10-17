package play.example.game.app.module.task.entity

import play.util.EmptyIntArray

/**
 * 玩家任务基类
 * @author LiangZengle
 */
open class TaskData(val id: Int) {

  /**
   * 任务状态
   */
  var status = TaskStatus.Accepted

  /**
   * 任务进度{目标索引:进度值}
   */
  var progresses = EmptyIntArray

  /**
   * 改变任务进度
   *
   * @param index 目标索引
   * @param delta 变化值
   * @return 变化后的值
   */
  fun addProgress(index: Int, delta: Int): Int {
    val newProgress = getProgress(index) + delta
    setProgress(index, newProgress)
    return newProgress
  }

  /**
   * 进度加1
   *
   * @param index 目标索引
   * @return 变化后的值
   */
  fun increaseProgress(index: Int): Int = addProgress(index, 1)

  /**
   * 设置任务进度
   *
   * @param index 目标索引
   * @param progress 进度值
   */
  fun setProgress(index: Int, progress: Int) {
    if (progresses.size <= index) {
      progresses = progresses.copyOf(index + 1)
    }
    progresses[index] = progress
  }

  /**
   * 是否进行中
   */
  fun isInProgress() = status == TaskStatus.Accepted

  /**
   * 是否已完成
   */
  fun isFinished() = status == TaskStatus.Finished

  /**
   * 是否已领奖
   */
  fun isRewarded() = status == TaskStatus.Rewarded

  /**
   * 是否已经完成或者已领奖
   */
  fun isFinishedOrRewarded() = isFinished() || isRewarded()

  /**
   * 设置为完成状态
   */
  fun setFinished() {
    status = TaskStatus.Finished
  }

  /**
   * 设置为已领奖状态
   */
  fun setRewarded() {
    status = TaskStatus.Rewarded
  }

  /**
   * 获取目标进度值
   *
   * @param index 目标索引
   * @return 目标进度
   */
  fun getProgress(index: Int): Int {
    return if (progresses.size > index) progresses[index] else 0
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(status=$status, progresses=${progresses.contentToString()})"
  }
}
