package play.example.game.app.module.servertask.domain

import play.codegen.EnumInterface
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.TaskTarget
import kotlin.reflect.KClass

@EnumInterface(TaskTargetType::class)
enum class ServerTaskTargetType(override val taskTargetClass: Class<out TaskTarget>): TaskTargetType {
//  PlayerCount(),
  ;

  constructor(kclass: KClass<out TaskTarget>) : this(kclass.java)
}
