package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmName
import kotlin.reflect.KClass

/**
 *
 * @author LiangZengle
 */
class FactoryExtensionGenerator {

  fun generate(pkg: String, fileName: String): FileSpec {
    return FileSpec.builder(pkg, fileName)
      .jvmName(fileName)
      .apply {
        listToArrayExtensions().forEach(this::addFunction)
      }
      .apply {
        ofAllListExtensions().forEach(this::addFunction)
        ofAllMapExtensions().forEach(this::addFunction)
      }
      .build()
  }

  private fun listToArrayExtensions(): List<FunSpec> {
    return Types.primitiveTypes.map { type ->
      FunSpec.builder("copyToArray")
        .receiver(List::class.parameterizedBy(type))
        .returns(Types.primitiveArrayTypes[type]!!)
        .addStatement("return %T(size) { this[it] }", Types.primitiveArrayTypes[type]!!)
        .build()
    }
  }

  private fun ofAllListExtensions(): List<FunSpec> {
    return Types.immutableListFactoryTypes.map { (elementType, factoryType) ->
      FunSpec.builder("ofList")
        .receiver(factoryType)
        .addParameter("list", List::class.parameterizedBy(elementType))
        .returns(Types.immutableListTypes[elementType]!!)
        .addStatement("return %M(*list.copyToArray())", Types.newListWith)
        .build()
    }
  }

  private fun ofAllMapExtensions(): List<FunSpec> {
    val K = TypeVariableName("K")
    val V = TypeVariableName("V")
    return Types.immutableMapFactoryTypes.map { (kv, factoryType) ->
      val (keyType, valueType) = kv
      val rawMapType = Types.immutableMapTypes[kv]!!
      if(Types.isObj(keyType) && Types.isObj(valueType)) {
        FunSpec.builder("ofAll")
          .addTypeVariables(listOf(K, V))
          .receiver(factoryType)
          .returns(rawMapType.parameterizedBy(keyType, valueType))
          .addParameter("map", Map::class.asClassName().parameterizedBy(K, V))
          .addStatement("return %T.immutable.from(map.entries, { it.key }, { it.value })", getMapsType(keyType, valueType))
          .build()
      } else if(Types.isObj(keyType)) {
        FunSpec.builder("ofAll")
          .addTypeVariable(K)
          .receiver(factoryType)
          .returns(rawMapType.parameterizedBy(keyType))
          .addParameter("map", Map::class.asClassName().parameterizedBy(K, valueType.asClassName()))
          .addStatement("return %T.immutable.from(map.entries, { it.key }, { it.value })", getMapsType(keyType, valueType))
          .build()
      } else if(Types.isObj(valueType)) {
        FunSpec.builder("ofAll")
          .addTypeVariable(V)
          .receiver(factoryType)
          .returns(rawMapType.parameterizedBy(valueType))
          .addParameter("map", Map::class.asClassName().parameterizedBy(keyType.asClassName(), V))
          .addStatement("return %T.immutable.from(map.entries, { it.key }, { it.value })", getMapsType(keyType, valueType))
          .build()
      } else {
        FunSpec.builder("ofAll")
          .receiver(factoryType)
          .returns(rawMapType)
          .addParameter("map", Map::class.parameterizedBy(keyType, valueType))
          .addStatement("return %T.immutable.from(map.entries, { it.key }, { it.value })", getMapsType(keyType, valueType))
          .build()
      }
     }
  }

  private fun getMapsType(keyType_: KClass<*>, valueType_: KClass<*>): ClassName {
    val keyType = if (Types.isObj(keyType_)) "Object" else keyType_.simpleName
    val valueType = if (Types.isObj(valueType_)) "Object" else valueType_.simpleName
    val simpleName = keyType + valueType + "Maps"
    return ClassName.bestGuess("org.eclipse.collections.api.factory.primitive.$simpleName")
  }
}
