package play.example.game.app.module.task.domain

import play.codegen.EnumInterface
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.target.NonTarget
import play.example.game.app.module.task.target.TaskTarget
import kotlin.reflect.KClass

@EnumInterface(TaskTargetType::class)
enum class CommonTaskTargetType(override val taskTargetClass: Class<out TaskTarget>): TaskTargetType {
  None(NonTarget::class),
  ;

  constructor(kclass: KClass<out TaskTarget>) : this(kclass.java)
}
