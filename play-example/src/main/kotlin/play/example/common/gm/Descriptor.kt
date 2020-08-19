package play.example.common.gm

data class GmCommandDescriptor(
  val name: String,
  val desc: String,
  val args: List<GmCommandArgDescriptor>
)

data class GmCommandArgDescriptor(val name: String, val desc: String, val defaultValue: String?)

