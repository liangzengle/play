package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class MutableSetGenerator {

  fun generate(elemType: KClass<*>, eclipseCollectionType: KClass<*>, eclipseMutableIteratorType: TypeName): TypeSpec {
    return TypeSpec.classBuilder("Mutable${elemType.simpleName}SetWrapper")
      .superclass(java.util.AbstractSet::class.parameterizedBy(elemType))
      .addSuperinterface(MutableSet::class.parameterizedBy(elemType))
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
        FunSpec.builder("add")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("element", elemType)
          .returns(Boolean::class)
          .addStatement("return underlying.add(element)")
          .build()
      )
      .addFunction(
        FunSpec.builder("clear")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return underlying.clear()")
          .build()
      )
      .addFunction(
        FunSpec.builder("remove")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("element", elemType)
          .addStatement("return underlying.remove(element)")
          .build()
      )
      .addFunction(
        FunSpec.builder("contains")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("element", elemType)
          .addStatement("return underlying.contains(element)")
          .build()
      )
      .addFunction(
        FunSpec.builder("toString")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return underlying.toString()")
          .build()
      )
      .addFunction(
        FunSpec.builder("iterator")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Types.mutableIteratorOf(elemType))
          .addStatement("return It(underlying)")
          .build()
      )
      .addFunction(
        FunSpec.builder("unwrap")
          .returns(eclipseCollectionType)
          .addStatement("return underlying")
          .build()
      )
      .addType(
        TypeSpec.classBuilder("It")
          .addModifiers(KModifier.PRIVATE)
          .addSuperinterface(Types.mutableIteratorOf(elemType))
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("underlying", eclipseCollectionType)
              .build()
          )
          .addProperty(
            PropertySpec.builder("it", eclipseMutableIteratorType)
              .initializer("underlying.${elemType.simpleName!!.replaceFirstChar { it.lowercaseChar() }}Iterator()")
              .build()
          )
          .addFunction(
            FunSpec.builder("hasNext")
              .addModifiers(KModifier.OVERRIDE)
              .returns(Boolean::class)
              .addStatement("return it.hasNext()")
              .build()
          )
          .addFunction(
            FunSpec.builder("next")
              .addModifiers(KModifier.OVERRIDE)
              .returns(elemType)
              .addStatement("return it.next()")
              .build()
          )
          .addFunction(
            FunSpec.builder("remove")
              .addModifiers(KModifier.OVERRIDE)
              .addStatement("return it.remove()")
              .build()
          )
          .build()
      )
      .build()
  }
}
