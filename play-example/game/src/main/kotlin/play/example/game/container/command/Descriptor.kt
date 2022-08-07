package play.example.game.container.command

data class CommandDescriptor(
  val module: String,
  val name: String,
  val desc: String,
  val params: List<CommandParamDescriptor>
)

data class CommandParamDescriptor(val name: String, val desc: String, val defaultValue: String)

data class CommandModuleDescriptor(val name: String, val label: String, val commands: List<CommandDescriptor>)
