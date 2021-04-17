package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmName
import kotlin.reflect.KClass

/**
 *
 * @author LiangZengle
 */
class AdapterGenerator {

  fun generate(pkg: String, fileName: String): FileSpec {
    fun typeOfImmutableCollection(elemType: KClass<*>, collectionType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.collection.immutable.${collectionType.simpleName}Wrapper")
    }

    fun typeOfImmutableList(elemType: KClass<*>, collectionType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.list.immutable.${collectionType.simpleName}Wrapper")
    }

    fun typeOfImmutableSet(elemType: KClass<*>, collectionType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.set.immutable.${collectionType.simpleName}Wrapper")
    }

    fun typeOfImmutableMap(keyType: KClass<*>, valueType: KClass<*>, collectionType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.map.immutable.${collectionType.simpleName}Wrapper")
    }
    return FileSpec.builder(pkg, fileName)
      .jvmName("Adaptor")
      .apply {
        for ((elemType, collectionType) in Types.immutableCollectionTypes) {
          addFunction(
            FunSpec.builder("toJava")
              .receiver(collectionType)
              .returns(Collection::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableCollection(elemType, collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.immutableListTypes) {
          addFunction(
            FunSpec.builder("toJava")
              .receiver(collectionType)
              .returns(List::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableList(elemType, collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.immutableSetTypes) {
          addFunction(
            FunSpec.builder("toJava")
              .receiver(collectionType)
              .returns(Set::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableSet(elemType, collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((keyType, valueType, collectionType) in Types.immutableMapTypes) {
          addFunction(
            FunSpec.builder("toJava")
              .apply {
                if (Types.isObj(keyType)) {
                  addTypeVariable(TypeVariableName("K"))
                } else if (Types.isObj(valueType)) {
                  addTypeVariable(TypeVariableName("V"))
                }
              }
              .receiver(
                when {
                    Types.isObj(keyType) -> {
                      collectionType.asTypeName().parameterizedBy(TypeVariableName("K"))
                    }
                    Types.isObj(valueType) -> {
                      collectionType.asTypeName().parameterizedBy(TypeVariableName("V"))
                    }
                    else -> collectionType.asTypeName()
                }
              )
              .returns(
                when {
                  Types.isObj(keyType) -> {
                    Map::class.asTypeName().parameterizedBy(TypeVariableName("K"),valueType.asTypeName())
                  }
                  Types.isObj(valueType) -> {
                    Map::class.asTypeName().parameterizedBy(keyType.asTypeName(), TypeVariableName("V"))
                  }
                  else -> Map::class.parameterizedBy(keyType, valueType)
                }
              )
              .addStatement("return %T(this)", typeOfImmutableMap(keyType, valueType, collectionType))
              .build()
          )
        }
      }
      .build()
  }
}
