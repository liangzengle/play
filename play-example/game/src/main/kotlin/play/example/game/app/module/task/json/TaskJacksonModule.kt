package play.example.game.app.module.task.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.auto.service.AutoService
import play.example.game.app.module.task.domain.TaskTargetType

@AutoService(Module::class)
class TaskJacksonModule: SimpleModule() {
  init {
    addDeserializer(TaskTargetType::class.java, TaskTargetTypeDeserializer)
  }
}
