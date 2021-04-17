package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class ImmutableSetGenerator {

  fun generate(elemType: KClass<*>, eclipseCollectionType: KClass<*>, iteratorImplType: TypeName): TypeSpec {
    return TypeSpec.classBuilder("Immutable${elemType.simpleName}SetWrapper")
      .superclass(AbstractSet::class.asTypeName().parameterizedBy(elemType.asTypeName()))
      .addSuperinterface(Set::class.asTypeName().parameterizedBy(elemType.asTypeName()))
      .primaryConstructor(
        FunSpec.constructorBuilder()
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
        FunSpec.builder("isEmpty")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Boolean::class)
          .addStatement("return underlying.isEmpty")
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
          .addParameter("elements", Collection::class.parameterizedBy(elemType))
          .returns(Boolean::class)
          .addStatement("return elements.all(::contains)")
          .build()
      )
      .addFunction(
        FunSpec.builder("iterator")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Iterator::class.asClassName().parameterizedBy(elemType.asClassName()))
          .addStatement("return %T(underlying.${elemType.simpleName!!.decapitalize()}Iterator())", iteratorImplType)
          .build()
      )
      .addFunction(
        FunSpec.builder("unwrap")
          .returns(eclipseCollectionType)
          .addStatement("return underlying")
          .build()
      )
      .build()
  }
}
