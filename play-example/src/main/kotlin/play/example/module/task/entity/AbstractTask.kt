package play.example.module.task.entity

import org.eclipse.collections.impl.factory.primitive.IntIntMaps

/**
 * 玩家任务基类
 * @author LiangZengle
 */
abstract class AbstractTask(val id: Int) {

  /**
   * 任务状态
   */
  var status = TaskStatus.Accepted

  /**
   * 任务进度{目标索引:进度值}
   */
  val progresses = IntIntMaps.mutable.empty()

  /**
   * 改变任务进度
   *
   * @param index 目标索引
   * @param change 变化值
   * @return 变化后的值
   */
  fun changeProgress(index: Int, change: Int): Int {
    return progresses.updateValue(index, change) { it + change }
  }

  /**
   * 进度加1
   *
   * @param index 目标索引
   * @return 变化后的值
   */
  fun increaseProgress(index: Int): Int = changeProgress(index, 1)

  /**
   * 设置任务进度
   *
   * @param index 目标索引
   * @param progress 进度值
   */
  fun setProgress(index: Int, progress: Int) = progresses.put(index, progress)

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
  fun isFinishedOrRewarded() = status != TaskStatus.Accepted

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
  fun getProgress(index: Int): Int = progresses[index]

  /**
   * 获取有进度值的目标数量
   */
  fun progressesSize(): Int = progresses.size()
}
