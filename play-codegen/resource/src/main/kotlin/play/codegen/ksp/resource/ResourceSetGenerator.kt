package play.codegen.ksp.resource

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeSpec
import play.codegen.*
import play.codegen.ksp.*
import play.codegen.ksp.resource.component.*

class ResourceSetGenerator(environment: SymbolProcessorEnvironment) : AbstractSymbolProcessor(environment) {

  private val typeSpecSet = hashSetOf<TypeSpecWithPackage>()

  override fun process(): List<KSAnnotated> {
    val subclasses = resolver.getAllSubclasses(AbstractResource.canonicalName).toSet()
    for (entityClass in subclasses) {
      if (!entityClass.isAnnotationPresent(DisableCodegen)) {
        val typeSpec = generate(entityClass)
        typeSpecSet.add(typeSpec)
      }
    }
    return emptyList()
  }

  override fun finish() {
    for (o in typeSpecSet) {
      write(o)
    }
  }

  private fun generate(ksClassDeclaration: KSClassDeclaration): TypeSpecWithPackage {
    val simpleName = ksClassDeclaration.simpleName.asString()
    val isSingleton = isSingleton(ksClassDeclaration)
    val postfix = if (isSingleton) "Conf" else "Set"
    val objectName = simpleName + postfix
    val objectBuilder = TypeSpec.objectBuilder(objectName)
      .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
    if (isSingleton) {
      SingletonResourceSetComponent(ksClassDeclaration).apply(objectBuilder)
    } else {
      ResourceSetComponent(ksClassDeclaration, resolver).apply(objectBuilder)
      if (hasExtension(ksClassDeclaration)) {
        ResourceExtensionComponent(ksClassDeclaration).apply(objectBuilder)
      }
      if (hasUniqueKey(ksClassDeclaration)) {
        UniqueKeyResourceSetComponent(ksClassDeclaration, resolver).apply(objectBuilder)
      }
      if (isGrouped(ksClassDeclaration)) {
        GroupedResourceSetComponent(ksClassDeclaration, resolver).apply(objectBuilder)
      }
    }
    return TypeSpecWithPackage(objectBuilder.build(), ksClassDeclaration.packageName.asString())
  }

  private fun isGrouped(ksClassDeclaration: KSClassDeclaration): Boolean {
    return resolver.isAssignable(Grouped.canonicalName, ksClassDeclaration)
  }

  private fun hasExtension(ksClassDeclaration: KSClassDeclaration): Boolean {
    return resolver.isAssignable(ExtensionKey.canonicalName, ksClassDeclaration)
  }

  private fun hasUniqueKey(ksClassDeclaration: KSClassDeclaration): Boolean {
    return resolver.isAssignable(UniqueKey.canonicalName, ksClassDeclaration)
  }

  private fun isSingleton(ksClassDeclaration: KSClassDeclaration): Boolean {
    return resolver.isAssignable(
      AbstractConfig.canonicalName, ksClassDeclaration
    ) || ksClassDeclaration.isAnnotationPresent(SingletonResource)
  }
}
