package play.codegen.ksp.resource.component

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import play.codegen.ExtensionKey
import play.codegen.ExtensionResourceSet
import play.codegen.ksp.getTypeArg

/**
 *
 * @author LiangZengle
 */
class ResourceExtensionComponent(private val classDeclaration: KSClassDeclaration) {

  fun apply(classBuilder: TypeSpec.Builder) {
    val extensionType = getExtensionType()
    classBuilder.addFunction(
      FunSpec.builder("extension").addModifiers(KModifier.PUBLIC).addAnnotation(JvmStatic::class).returns(extensionType)
        .addStatement(
          "return (underlying.getDelegatee() as %T<%T, %T>).extension()",
          ExtensionResourceSet,
          extensionType,
          classDeclaration.toClassName()
        ).build()
    )
  }

  private fun getExtensionType(): TypeName {
    return try {
      classDeclaration.getTypeArg(ExtensionKey.canonicalName, 0)
    } catch (e: Exception) {
      throw IllegalStateException("Extension type not found for ${classDeclaration.qualifiedName?.asString()}", e)
    }
  }
}
