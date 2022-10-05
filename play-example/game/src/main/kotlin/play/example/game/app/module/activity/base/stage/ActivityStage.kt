package play.example.game.app.module.activity.base.stage

/**
 *
 * @author LiangZengle
 */
enum class ActivityStage(val handler: ActivityStageHandler) {
  None(NoneStageHandler),
  Init(InitStageHandler),
  Start(StartStageHandler),
  End(EndStageHandler),
  Close(CloseStageHandler);

  companion object {
    @JvmStatic
    val VALUES = values()

    @JvmStatic
    fun contains(stages: Int, stage: ActivityStage): Boolean = (stages and stage.identifier) != 0

    @JvmStatic
    fun contains(stages: Int, target: Int): Boolean = (stages and target) != 0

    @JvmStatic
    fun just(stage: ActivityStage): Int = stage.identifier

    @JvmStatic
    fun or(stage1: ActivityStage, stage2: ActivityStage): Int = stage1.identifier or stage2.identifier

    @JvmStatic
    fun any(): Int = VALUES.fold(0) { r, v -> r or v.identifier }
  }

  val identifier = 1 shl (ordinal)

  fun prev(): ActivityStage? = if (ordinal in 1..< VALUES.size) VALUES[ordinal - 1] else null

  fun next(): ActivityStage? = if (ordinal in 0 ..< VALUES.size - 1) VALUES[ordinal + 1] else null

  infix fun or(other: ActivityStage): Int {
    return identifier or other.identifier
  }

  infix fun or(stages: Int): Int {
    return identifier or stages
  }
}
