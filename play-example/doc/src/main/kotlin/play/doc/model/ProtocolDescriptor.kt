package play.doc.model

import play.dokka.model.ParameterDescriptor

/**
 *
 * @author LiangZengle
 */
data class ProtocolDescriptor(
  val id: Int,
  val type: Int,
  val desc: String,
  val parameters: List<ParameterDescriptor>,
  val returnType: String,
  val returnDesc: String
)
