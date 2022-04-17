package play.example.game.container.command

import kotlinx.serialization.Serializable

@Serializable
data class CommandDescriptor(
  val module: String,
  val name: String,
  val desc: String,
  val params: List<CommandParamDescriptor>
)

@Serializable
data class CommandParamDescriptor(val name: String, val desc: String, val defaultValue: String)

@Serializable
data class CommandModuleDescriptor(val name: String, val label: String, val commands: List<CommandDescriptor>)
