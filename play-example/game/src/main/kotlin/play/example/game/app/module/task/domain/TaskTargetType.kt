package play.example.game.app.module.task.domain

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import play.example.game.app.module.task.target.NonTarget
import play.example.game.app.module.task.target.PlayerLevelTaskTarget
import play.example.game.app.module.task.target.TaskTarget
import kotlin.reflect.KClass

/**
 * 任务目标类型
 * @property taskTargetClass 任务目标的Class
 * @author LiangZengle
 */
enum class TaskTargetType(val taskTargetClass: Class<out TaskTarget>) {
  /**
   * 空目标，占位用，防止配置报错
   */
  @JsonEnumDefaultValue
  None(NonTarget::class),

  /**
   * 等级
   */
  PlayerLevel(PlayerLevelTaskTarget::class),
  ;

  constructor(kclass: KClass<out TaskTarget>) : this(kclass.java)
}
