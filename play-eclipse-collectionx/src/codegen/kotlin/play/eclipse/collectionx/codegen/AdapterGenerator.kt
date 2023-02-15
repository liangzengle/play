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
    fun typeOfImmutableCollection(collectionType: KClass<*>): TypeName {
      val typeName = if (collectionType.simpleName?.startsWith("Immutable") == false) {
        "org.eclipse.collectionx.collection.immutable.Immutable${collectionType.simpleName}Wrapper"
      } else {
        "org.eclipse.collectionx.collection.immutable.${collectionType.simpleName}Wrapper"
      }
      return ClassName.bestGuess(typeName)
    }

    fun typeOfImmutableList(collectionType: KClass<*>): TypeName {
      val typeName = if (collectionType.simpleName?.startsWith("Immutable") == false) {
        "org.eclipse.collectionx.list.immutable.Immutable${collectionType.simpleName}Wrapper"
      } else {
        "org.eclipse.collectionx.list.immutable.${collectionType.simpleName}Wrapper"
      }
      return ClassName.bestGuess(typeName)
    }

    fun typeOfImmutableSet(collectionType: KClass<*>): TypeName {
      val typeName = if (collectionType.simpleName?.startsWith("Immutable") == false) {
        "org.eclipse.collectionx.set.immutable.Immutable${collectionType.simpleName}Wrapper"
      } else {
        "org.eclipse.collectionx.set.immutable.${collectionType.simpleName}Wrapper"
      }
      return ClassName.bestGuess(typeName)
    }

    fun typeOfImmutableMap(mapType: KClass<*>): TypeName {
      val typeName = if (mapType.simpleName?.startsWith("Immutable") == false) {
        "org.eclipse.collectionx.map.immutable.Immutable${mapType.simpleName}Wrapper"
      } else {
        "org.eclipse.collectionx.map.immutable.${mapType.simpleName}Wrapper"
      }
      return ClassName.bestGuess(typeName)
    }

    fun typeOfMutableCollection(collectionType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.collection.mutable.${collectionType.simpleName}Wrapper")
    }

    fun typeOfMutableList(listType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.list.mutable.${listType.simpleName}Wrapper")
    }

    fun typeOfMutableSet(setType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.set.mutable.${setType.simpleName}Wrapper")
    }

    fun typeOfMutableMap(mapType: KClass<*>): TypeName {
      return ClassName.bestGuess("org.eclipse.collectionx.map.mutable.${mapType.simpleName}Wrapper")
    }

    val K = TypeVariableName("K", Any::class.asClassName())
    val V = TypeVariableName("V", Any::class.asClassName())

    return FileSpec.builder(pkg, fileName)
      .jvmName("Adaptor")
      .apply {
        for ((elemType, collectionType) in Types.immutableCollectionTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(Collection::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableCollection(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.listTypes + Types.immutableListTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(List::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableList(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.setTypes + Types.immutableSetTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(Set::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfImmutableSet(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((keyType, valueType, mapType) in Types.mapTypes + Types.immutableMapTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .apply {
                if (Types.isObj(keyType)) {
                  addTypeVariable(K)
                } else if (Types.isObj(valueType)) {
                  addTypeVariable(V)
                }
              }
              .receiver(
                when {
                  Types.isObj(keyType) -> {
                    mapType.asTypeName().parameterizedBy(K)
                  }
                  Types.isObj(valueType) -> {
                    mapType.asTypeName().parameterizedBy(V)
                  }
                  else -> mapType.asTypeName()
                }
              )
              .returns(
                when {
                  Types.isObj(keyType) -> {
                    Map::class.asTypeName().parameterizedBy(K, valueType.asTypeName())
                  }
                  Types.isObj(valueType) -> {
                    Map::class.asTypeName().parameterizedBy(keyType.asTypeName(), V)
                  }
                  else -> Map::class.parameterizedBy(keyType, valueType)
                }
              )
              .addStatement("return %T(this)", typeOfImmutableMap(mapType))
              .build()
          )
        }
      }

      // mutable
      .apply {
        for ((_, collectionType) in Types.mutableCollectionTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
//              .returns(Collection::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfMutableCollection(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((_, collectionType) in Types.mutableListTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
//              .returns(List::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfMutableList(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((_, collectionType) in Types.mutableSetTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
//              .returns(Set::class.parameterizedBy(elemType))
              .addStatement("return %T(this)", typeOfMutableSet(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((keyType, valueType, mapType) in Types.mutableMapTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .apply {
                if (Types.isObj(keyType)) {
                  addTypeVariable(K)
                } else if (Types.isObj(valueType)) {
                  addTypeVariable(V)
                }
              }
              .receiver(
                when {
                  Types.isObj(keyType) -> {
                    mapType.asTypeName().parameterizedBy(K)
                  }
                  Types.isObj(valueType) -> {
                    mapType.asTypeName().parameterizedBy(V)
                  }
                  else -> mapType.asTypeName()
                }
              )
//              .returns(
//                when {
//                  Types.isObj(keyType) -> {
//                    Map::class.asTypeName().parameterizedBy(K, valueType.asTypeName())
//                  }
//                  Types.isObj(valueType) -> {
//                    Map::class.asTypeName().parameterizedBy(keyType.asTypeName(), V)
//                  }
//                  else -> Map::class.parameterizedBy(keyType, valueType)
//                }
//              )
              .addStatement("return %T(this)", typeOfMutableMap(mapType))
              .build()
          )
        }
      }
      .build()
  }
}
