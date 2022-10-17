package play.example.game.app.module.playertask.domain

import play.codegen.EnumInterface
import play.example.game.app.module.playertask.target.PlayerLevelTaskTarget
import play.example.game.app.module.playertask.target.PlayerLoginTaskTarget
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.TaskTarget
import kotlin.reflect.KClass

@EnumInterface(TaskTargetType::class)
enum class PlayerTaskTargetType(override val taskTargetClass: Class<out TaskTarget>) : TaskTargetType {
  PlayerLevel(PlayerLevelTaskTarget::class),
  PlayerLogin(PlayerLoginTaskTarget::class),
  ;

  constructor(kclass: KClass<out TaskTarget>) : this(kclass.java)
}
