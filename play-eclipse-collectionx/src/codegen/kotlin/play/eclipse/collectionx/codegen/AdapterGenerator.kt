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
        for ((elemType, collectionType) in Types.listTypes.entries + Types.immutableListTypes.entries) {
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
        for ((elemType, collectionType) in Types.setTypes.entries + Types.immutableSetTypes.entries) {
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
        for ((kv, mapType) in Types.mapTypes.entries + Types.immutableMapTypes.entries) {
          val (keyType, valueType) = kv
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
        for ((elemType, collectionType) in Types.mutableCollectionTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(MUTABLE_COLLECTION.parameterizedBy(elemType.asTypeName()))
              .addStatement("return %T(this)", typeOfMutableCollection(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.mutableListTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(MUTABLE_LIST.parameterizedBy(elemType.asTypeName()))
              .addStatement("return %T(this)", typeOfMutableList(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((elemType, collectionType) in Types.mutableSetTypes) {
          addFunction(
            FunSpec.builder("asJava")
              .receiver(collectionType)
              .returns(MUTABLE_SET.parameterizedBy(elemType.asTypeName()))
              .addStatement("return %T(this)", typeOfMutableSet(collectionType))
              .build()
          )
        }
      }
      .apply {
        for ((kv, mapType) in Types.mutableMapTypes) {
          val (keyType, valueType) = kv
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
                    MUTABLE_MAP.parameterizedBy(K, valueType.asTypeName())
                  }

                  Types.isObj(valueType) -> {
                    MUTABLE_MAP.parameterizedBy(keyType.asTypeName(), V)
                  }

                  else -> MUTABLE_MAP.parameterizedBy(keyType.asTypeName(), valueType.asTypeName())
                }
              )
              .addStatement("return %T(this)", typeOfMutableMap(mapType))
              .build()
          )
        }
      }
      .build()
  }
}
