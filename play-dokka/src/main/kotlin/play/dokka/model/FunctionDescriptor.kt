package play.dokka.model

/**
 *
 * @author LiangZengle
 */
data class FunctionDescriptor(
  val name: String,
  val desc: String,
  val parameters: List<ParameterDescriptor>,
  val returnDesc: String
)
