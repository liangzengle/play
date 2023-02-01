package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class IteratorGenerator {

  fun generate(elemType: KClass<*>, iteratorType: KClass<*>): TypeSpec {
    return TypeSpec.classBuilder("${elemType.simpleName}IteratorWrapper")
      .addSuperinterface(Iterator::class.asTypeName().parameterizedBy(elemType.asTypeName()))
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("underlying", iteratorType)
          .build()
      )
      .addProperty(
        PropertySpec.builder("underlying", iteratorType, KModifier.PRIVATE)
          .initializer("underlying")
          .build()
      )
      .addFunction(
        FunSpec.builder("hasNext")
          .returns(Boolean::class)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return underlying.hasNext()")
          .build()
      )
      .addFunction(
        FunSpec.builder("next")
          .returns(elemType)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return underlying.next()")
          .build()
      )
      .addFunction(
        FunSpec.builder("next" + elemType.simpleName)
          .returns(elemType)
          .addStatement("return underlying.next()")
          .build()
      )
      .addFunction(
        FunSpec.builder("unwrap")
          .returns(iteratorType)
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
      .build()
  }
}
