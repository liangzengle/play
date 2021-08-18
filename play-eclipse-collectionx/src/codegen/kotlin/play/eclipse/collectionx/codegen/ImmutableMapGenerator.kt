package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.function.BiConsumer
import kotlin.reflect.KClass

/**
 * @author LiangZengle
 */
class ImmutableMapGenerator {

  fun generate(
    _keyType: KClass<*>,
    _valueType: KClass<*>,
    _eclipseCollectionType: KClass<*>,
    keySetType: TypeName,
    valueCollectionType: TypeName,
    unmodifiableKeySetType: TypeName,
    unmodifiableValueCollectionType: TypeName,
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
    return TypeSpec.classBuilder("Immutable${Types.simpleNameOf(_keyType)}${Types.simpleNameOf(_valueType)}MapWrapper")
      .addSuperinterface(Map::class.asTypeName().parameterizedBy(keyType, valueType))
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
          Set::class.asClassName().parameterizedBy(keyType),
          KModifier.OVERRIDE
        )
          .getter(
            FunSpec.getterBuilder()
              .apply {
                if (Types.isObj(_keyType)) {
                  addStatement("return %T.of(underlying.keySet())", unmodifiableKeySetType)
                } else {
                  addStatement("return %T(%T.of(underlying.keySet()))", keySetType, unmodifiableKeySetType)
                }
              }
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder(
          "values",
          Collection::class.asClassName().parameterizedBy(valueType),
          KModifier.OVERRIDE
        )
          .getter(
            FunSpec.getterBuilder()
              .apply {
                if (Types.isObj(_valueType)) {
                  addStatement("return %T.of(underlying.values())", unmodifiableValueCollectionType)
                } else {
                  addStatement(
                    "return %T(%T.of(underlying.values()))",
                    valueCollectionType,
                    unmodifiableValueCollectionType
                  )
                }
              }
              .build()
          )
          .build()
      )
      .addProperty(
        PropertySpec.builder(
          "entries",
          Set::class.asClassName().parameterizedBy(
            Map.Entry::class.asClassName().parameterizedBy(keyType, valueType)
          ),
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
          .addStatement("return underlying.get(key)")
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
      .addFunction(
        FunSpec.builder("unwrap")
          .returns(eclipseCollectionType)
          .addStatement("return underlying")
          .build()
      )
      .addType(
        TypeSpec.classBuilder("Entry")
          .addSuperinterface(
            Map.Entry::class.asClassName().parameterizedBy(keyType, valueType)
          )
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
              .build()
          )
          .build()
      )
      .addType(
        TypeSpec.classBuilder("EntrySet")
          .apply {
            if (keyType is TypeVariableName) addTypeVariable(keyType)
            if (valueType is TypeVariableName) addTypeVariable(valueType)
          }
          .superclass(
            AbstractSet::class.asClassName().parameterizedBy(
              Map.Entry::class.asClassName().parameterizedBy(keyType, valueType)
            )
          )
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
              .addParameter("element", Map.Entry::class.asTypeName().parameterizedBy(keyType, valueType))
              .returns(Boolean::class)
              .addStatement("return underlying.containsKey(element.key) && underlying.get(element.key) == element.value")
              .build()
          )
          .addFunction(
            FunSpec.builder("iterator")
              .addModifiers(KModifier.OVERRIDE)
              .returns(
                Iterator::class.asClassName().parameterizedBy(
                  Map.Entry::class.asTypeName().parameterizedBy(keyType, valueType)
                )
              )
              .addStatement("return It(underlying)")
              .build()
          )
          .build()
      )
      .addType(
        TypeSpec.classBuilder("It")
          .apply {
            if (keyType is TypeVariableName) addTypeVariable(keyType)
            if (valueType is TypeVariableName) addTypeVariable(valueType)
          }
          .addSuperinterface(
            Iterator::class.asClassName().parameterizedBy(
              Map.Entry::class.asClassName().parameterizedBy(keyType, valueType)
            )
          )
          .primaryConstructor(
            FunSpec.constructorBuilder()
              .addParameter("underlying", eclipseCollectionType)
              .build()
          )
          .addProperty(
            PropertySpec.builder(
              "it",
              Iterator::class.asClassName().parameterizedBy(
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
              .returns(Map.Entry::class.asClassName().parameterizedBy(keyType, valueType))
              .addStatement("val pair = it.next()")
              .addStatement("return Entry(pair.one, pair.two)")
              .build()
          )
          .build()
      )
      .build()
  }
}
