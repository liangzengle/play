package play.codegen.ksp.controller.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import play.codegen.Cmd
import play.codegen.Controller
import play.codegen.ksp.getAnnotation
import play.codegen.ksp.getValue
import play.codegen.ksp.isAnnotationPresent
import play.codegen.uncapitalize2

class ControllerClassDeclaration(
    val moduleId: Short,
    val underlying: KSClassDeclaration,
    val invokerClassName: ClassName,
    val commandFunctions: List<CommandFunctionDeclaration>,
    val commandProperties: List<CommandPropertyDeclaration>
) : KSClassDeclaration by underlying {
  companion object {
    operator fun invoke(ksClassDeclaration: KSClassDeclaration): ControllerClassDeclaration {
      val moduleId = ksClassDeclaration.getAnnotation(Controller).getValue<Short>("moduleId")
      val cmdFunctions = ksClassDeclaration.getAllFunctions().filter { it.isAnnotationPresent(Cmd) }.map {
        val cmd = it.getAnnotation(Cmd)
        val commandId = cmd.getValue<Byte>("value")
        val dummy = cmd.getValue<Boolean>("dummy")
        CommandFunctionDeclaration(it, commandId, dummy)
      }.toList()
      val cmdProperties = ksClassDeclaration.getAllProperties().filter { it.isAnnotationPresent(Cmd) }.map {
        val cmd = it.getAnnotation(Cmd)
        val commandId = cmd.getValue<Byte>("value")
        val dummy = cmd.getValue<Boolean>("dummy")
        CommandPropertyDeclaration(it, commandId, dummy)
      }.toList()
      val invokerClassName =
        ClassName.bestGuess(ksClassDeclaration.qualifiedName!!.asString().removeSuffix("Controller") + "Module")
      return ControllerClassDeclaration(
        moduleId, ksClassDeclaration, invokerClassName, cmdFunctions, cmdProperties
      )
    }
  }

  val invokerVarName = invokerClassName.simpleName.uncapitalize2()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ControllerClassDeclaration

    if (underlying != other.underlying) return false

    return true
  }

  override fun hashCode(): Int {
    return underlying.hashCode()
  }
}
