package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class MutableListGenerator {

  fun generate(elemType: KClass<*>, eclipseCollectionType: KClass<*>): TypeSpec {
    return TypeSpec.classBuilder("Immutable${elemType.simpleName}ListWrapper")
      .superclass(java.util.AbstractList::class.parameterizedBy(elemType))
      .addSuperinterface(MutableList::class.parameterizedBy(elemType))
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
        FunSpec.builder("get")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("index", Int::class)
          .returns(elemType)
          .addStatement("return underlying.get(index)")
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
        FunSpec.builder("add")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("index", Int::class)
          .addParameter("element", elemType)
          .addStatement("underlying.addAtIndex(index, element)")
          .build()
      )
      .addFunction(
        FunSpec.builder("addAll")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("elements", Collection::class.parameterizedBy(elemType))
          .returns(Boolean::class)
          .addStatement("return underlying.addAll(*elements.to${elemType.simpleName}Array())")
          .build()
      )
      .addFunction(
        FunSpec.builder("addAll")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("index", Int::class)
          .addParameter("elements", Collection::class.parameterizedBy(elemType))
          .returns(Boolean::class)
          .addStatement("return underlying.addAllAtIndex(index, *elements.to${elemType.simpleName}Array())")
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
        FunSpec.builder("removeAt")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("index", Int::class)
          .addStatement("return underlying.removeAtIndex(index)")
          .build()
      )
      .addFunction(
        FunSpec.builder("set")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("index", Int::class)
          .addParameter("element", elemType)
          .addStatement("return underlying.set(index, element)")
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
