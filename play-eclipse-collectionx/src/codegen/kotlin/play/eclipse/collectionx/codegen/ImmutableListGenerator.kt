package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class ImmutableListGenerator {

  fun generate(elemType: KClass<*>, eclipseCollectionType: KClass<*>): TypeSpec {
    return TypeSpec.classBuilder("Immutable${elemType.simpleName}ListWrapper")
      .superclass(AbstractList::class.asTypeName().parameterizedBy(elemType.asTypeName()))
      .addSuperinterface(List::class.asTypeName().parameterizedBy(elemType.asTypeName()))
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
        FunSpec.builder("unwrap")
          .returns(eclipseCollectionType)
          .addStatement("return underlying")
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
        FunSpec.builder("equals")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("other", Any::class.asTypeName().copy(true))
          .returns(Boolean::class)
          .addStatement("return underlying.equals(other)")
          .build()
      )
      .addFunction(
        FunSpec.builder("hashCode")
          .addModifiers(KModifier.OVERRIDE)
          .returns(Int::class)
          .addStatement("return underlying.hashCode()")
          .build()
      )
      .build()
  }
}
