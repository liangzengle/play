package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.function.Consumer
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class ImmutableCollectionGenerator {

  fun generate(elemType: KClass<*>, eclipseCollectionType: KClass<*>, iteratorType: TypeName): TypeSpec {
    return TypeSpec.classBuilder("Immutable${elemType.simpleName}CollectionWrapper")
      .addModifiers(KModifier.INTERNAL)
      .addSuperinterface(Collection::class.asTypeName().parameterizedBy(elemType.asTypeName()))
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addModifiers(KModifier.INTERNAL)
          .addParameter("underlying", eclipseCollectionType)
          .build()
      )
      .addProperty(
        PropertySpec.builder("underlying", eclipseCollectionType, KModifier.PRIVATE)
          .initializer("underlying")
          .build()
      )
      .addProperty(
        PropertySpec.builder("size", Int::class, KModifier.OVERRIDE)
          .getter(
            FunSpec.getterBuilder()
              .addStatement("return underlying.size()")
              .build()
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("contains")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("element", elemType)
          .returns(Boolean::class)
          .addStatement("return underlying.contains(element)")
          .build()
      )
      .addFunction(
        FunSpec.builder("containsAll")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("elements", Collection::class.asTypeName().parameterizedBy(elemType.asTypeName()))
          .returns(Boolean::class)
          .addStatement("return elements.all(::contains)")
          .build()
      )
      .addFunction(
        FunSpec.builder("forEach")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("action", Consumer::class.asTypeName().parameterizedBy(WildcardTypeName.consumerOf(elemType)))
          .addStatement("return underlying.forEach(action::accept)")
          .build()
      )
      .addFunction(
        FunSpec.builder("isEmpty")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Boolean::class)
          .addStatement("return underlying.isEmpty")
          .build()
      )
      .addFunction(
        FunSpec.builder("toString")
          .addModifiers(KModifier.OVERRIDE)
          .returns(String::class)
          .addStatement("return underlying.toString()")
          .build()
      )
      .addFunction(
        FunSpec.builder("iterator")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Iterator::class.parameterizedBy(elemType))
          .addStatement(
            "return %T(underlying.%LIterator())",
            iteratorType,
            elemType.simpleName!!.replaceFirstChar { it.lowercaseChar() })
          .build()
      )
      .build()
  }
}
