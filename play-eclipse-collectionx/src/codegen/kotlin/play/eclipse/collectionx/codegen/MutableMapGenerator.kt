package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.function.BiConsumer
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class MutableMapGenerator {

  fun generate(
    _keyType: KClass<*>,
    _valueType: KClass<*>,
    _eclipseCollectionType: KClass<*>,
    keySetType: TypeName,
    valueCollectionType: TypeName
  ): TypeSpec {
    val keyType = if (Types.isObj(_keyType)) TypeVariableName("K", Any::class.asClassName()) else _keyType.asClassName()
    val valueType =
      if (Types.isObj(_valueType)) TypeVariableName("V", Any::class.asClassName()) else _valueType.asClassName()
    val eclipseCollectionType = when {
      Types.isObj(_keyType) -> {
        _eclipseCollectionType.asTypeName().parameterizedBy(keyType)
      }
      Types.isObj(_valueType) -> {
        _eclipseCollectionType.asTypeName().parameterizedBy(valueType)
      }
      else -> _eclipseCollectionType.asTypeName()
    }
    return TypeSpec.classBuilder("Mutable${Types.simpleNameOf(_keyType)}${Types.simpleNameOf(_valueType)}MapWrapper")
      .superclass(java.util.AbstractMap::class.asTypeName().parameterizedBy(keyType, valueType))
      .addSuperinterface(MutableMap::class.asTypeName().parameterizedBy(keyType, valueType))
      .apply {
        if (keyType is TypeVariableName) addTypeVariable(keyType)
        if (valueType is TypeVariableName) addTypeVariable(valueType)
      }
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
      .addProperty(
        PropertySpec.builder(
          "keys",
          Types.mutableSetOf(keyType),
          KModifier.OVERRIDE
        )
          .getter(
            FunSpec.getterBuilder()
              .apply {
                if (Types.isObj(_keyType)) {
                  addStatement("return underlying.keySet()")
                } else {
                  addStatement("return %T(underlying.keySet())", keySetType)
                }
              }
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder(
          "values",
          Types.mutableCollectionOf(valueType),
          KModifier.OVERRIDE
        )
          .getter(
            FunSpec.getterBuilder()
              .apply {
                if (Types.isObj(_valueType)) {
                  addStatement("return underlying.values()")
                } else {
                  addStatement("return %T(underlying.values())", valueCollectionType)
                }
              }
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder(
          "entries",
          Types.mutableSetOf(Types.mutableEntryOf(keyType, valueType)),
          KModifier.OVERRIDE
        )
          .getter(
            FunSpec.getterBuilder()
              .addStatement("return EntrySet(underlying)")
              .build()
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("containsKey")
          .returns(Boolean::class)
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .addStatement("return underlying.containsKey(key)")
          .build()
      )
      .addFunction(
        FunSpec.builder("containsValue")
          .returns(Boolean::class)
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("value", valueType)
          .addStatement("return underlying.containsValue(value)")
          .build()
      )
      .addFunction(
        FunSpec.builder("forEach")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter(
            "action",
            BiConsumer::class.asClassName()
              .parameterizedBy(WildcardTypeName.consumerOf(keyType), WildcardTypeName.consumerOf(valueType))
          )
          .addStatement("return underlying.forEachKeyValue{k, v -> action.accept(k, v)}")
          .build()
      )
      .addFunction(
        FunSpec.builder("get")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .returns(valueType.copy(true))
          .addStatement("return if(underlying.containsKey(key)) underlying.get(key) else null")
          .build()
      )
      .addFunction(
        FunSpec.builder("getOrDefault")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .addParameter("defaultValue", valueType)
          .returns(valueType)
          .apply {
            if (Types.isObj(_valueType)) {
              addStatement("return underlying.getIfAbsent(key){ defaultValue }")
            } else {
              addStatement("return underlying.getIfAbsent(key, defaultValue)")
            }
          }
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
        FunSpec.builder("put")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .addParameter("value", valueType)
          .returns(valueType.copy(true))
          .addCode(
            buildCodeBlock {
              beginControlFlow("val prev = if (underlying.containsKey(key))")
              addStatement("underlying.get(key)")
              nextControlFlow("else")
              addStatement("null")
              endControlFlow()
              addStatement("underlying.put(key, value)")
              addStatement("return prev")
            }
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("remove")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .returns(valueType.copy(true))
          .addCode(
            buildCodeBlock {
              beginControlFlow(" val prev = if (underlying.containsKey(key))")
              addStatement("underlying.get(key)")
              nextControlFlow("else")
              addStatement("null")
              endControlFlow()
              addStatement("underlying.remove(key)")
              addStatement("return prev")
            }
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("remove")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .addParameter("value", valueType)
          .returns(Boolean::class)
          .addCode(
            buildCodeBlock {
              beginControlFlow("if (underlying.containsKey(key) && underlying.get(key) == value)")
              addStatement("underlying.remove(key)")
              addStatement(" return true")
              endControlFlow()
              addStatement("return false")
            }
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("clear")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("underlying.clear()")
          .build()
      )
      .addFunction(
        FunSpec.builder("computeIfAbsent")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("key", keyType)
          .addParameter(
            "mappingFunction",
            java.util.function.Function::class.asTypeName()
              .parameterizedBy(WildcardTypeName.consumerOf(keyType), WildcardTypeName.producerOf(valueType))
          )
          .returns(valueType)
          .addStatement("return underlying.getIfAbsentPutWithKey(key, mappingFunction::apply)")
          .build()
      )
      .addType(
        TypeSpec.classBuilder("Entry")
          .addSuperinterface(Types.mutableEntryOf(keyType, valueType))
          .apply {
            if (keyType is TypeVariableName) addTypeVariable(keyType)
            if (valueType is TypeVariableName) addTypeVariable(valueType)
          }
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("key", keyType)
              .addParameter("value", valueType)
              .build()
          )
          .addProperty(
            PropertySpec.builder("key", keyType, KModifier.OVERRIDE)
              .initializer("key")
              .build()
          )
          .addProperty(
            PropertySpec.builder("value", valueType, KModifier.OVERRIDE)
              .initializer("value")
              .mutable(true)
              .build()
          )
          .addFunction(
            FunSpec.builder("setValue")
              .addModifiers(KModifier.OVERRIDE)
              .returns(valueType)
              .addParameter("newValue", valueType)
              .addStatement("val prev = this.value")
              .addStatement("this.value = newValue")
              .addStatement("return prev")
              .build()
          )
          .build()
      )
      .addType(
        TypeSpec.classBuilder("EntrySet")
          .superclass(
            java.util.AbstractSet::class.asClassName().parameterizedBy(Types.mutableEntryOf(keyType, valueType))
          )
          .addSuperinterface(Types.mutableSetOf(Types.mutableEntryOf(keyType, valueType)))
          .apply {
            if (keyType is TypeVariableName) addTypeVariable(keyType)
            if (valueType is TypeVariableName) addTypeVariable(valueType)
          }
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
            FunSpec.builder("clear")
              .addModifiers(KModifier.OVERRIDE)
              .addStatement("return underlying.clear()")
              .build()
          )
          .addFunction(
            FunSpec.builder("add")
              .addModifiers(KModifier.OVERRIDE)
              .returns(Boolean::class)
              .addParameter("element", Types.mutableEntryOf(keyType, valueType))
              .addCode(
                buildCodeBlock {
                  beginControlFlow("if (contains(element))")
                  addStatement("return false")
                  endControlFlow()
                  addStatement("underlying.put(element.key, element.value)")
                  addStatement("return true")
                }
              )
              .build()
          )
          .addFunction(
            FunSpec.builder("remove")
              .addModifiers(KModifier.OVERRIDE)
              .returns(Boolean::class)
              .addParameter("element", Types.mutableEntryOf(keyType, valueType))
              .addCode(
                buildCodeBlock {
                  beginControlFlow("if (!contains(element))")
                  addStatement("return false")
                  endControlFlow()
                  addStatement("underlying.remove(element.key)")
                  addStatement("return true")
                }
              )
              .build()
          )
          .addFunction(
            FunSpec.builder("contains")
              .addModifiers(KModifier.OVERRIDE)
              .returns(Boolean::class)
              .addParameter("element", Types.mutableEntryOf(keyType, valueType))
              .addStatement("return underlying.containsKey(element.key) && underlying.get(element.key) == element.value")
              .build()
          )
          .addFunction(
            FunSpec.builder("toString")
              .addModifiers(KModifier.OVERRIDE)
              .returns(String::class)
              .addStatement("return underlying.keyValuesView().toString()")
              .build()
          )
          .addFunction(
            FunSpec.builder("iterator")
              .addModifiers(KModifier.OVERRIDE)
              .returns(
                Types.mutableIteratorOf(Types.mutableEntryOf(keyType, valueType))
              )
              .addStatement("return It(underlying)")
              .build()
          )
          .build()
      )
      .addType(
        TypeSpec.classBuilder("It")
          .addSuperinterface(Types.mutableIteratorOf(Types.mutableEntryOf(keyType, valueType)))
          .apply {
            if (keyType is TypeVariableName) addTypeVariable(keyType)
            if (valueType is TypeVariableName) addTypeVariable(valueType)
          }
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("underlying", eclipseCollectionType)
              .build()
          )
          .addProperty(
            PropertySpec.builder(
              "it",
              Types.mutableIteratorOf(
                when {
                  Types.isObj(_keyType) -> Types.pairOf(_keyType, _valueType).asTypeName().parameterizedBy(keyType)
                  Types.isObj(_valueType) -> Types.pairOf(_keyType, _valueType).asTypeName().parameterizedBy(valueType)
                  else -> Types.pairOf(_keyType, _valueType).asClassName()
                }
              ),
              KModifier.PRIVATE
            )
              .initializer("underlying.keyValuesView().iterator()")
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
              .returns(Types.mutableEntryOf(keyType, valueType))
              .addStatement("val pair = it.next()")
              .addStatement("return Entry(pair.one, pair.two)")
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
      .build()
  }
}
