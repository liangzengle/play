package play.example.game.app.module.task.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import play.example.game.app.module.task.domain.TaskTargetType
import play.example.game.app.module.task.domain.TaskTargetTypes

object TaskTargetTypeDeserializer : StdDeserializer<TaskTargetType>(TaskTargetType::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TaskTargetType {
    return TaskTargetTypes.getByNameOrThrow(p.text)
  }
}
