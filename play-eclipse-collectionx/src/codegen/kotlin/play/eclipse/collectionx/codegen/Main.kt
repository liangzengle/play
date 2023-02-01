package play.eclipse.collectionx.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import java.io.File
import kotlin.reflect.KClass

/**
 *
 * @author LiangZengle
 */
fun main(args: Array<String>) {
  val dir = if (args.isNotEmpty()) args[0].replace('.', '/')
  else System.getProperty("user.dir") + "/play-eclipse-collectionx/src/main/kotlin"

  fun iteratorTypeName(elemType: KClass<*>): TypeName {
    return ClassName.bestGuess("org.eclipse.collectionx.iterator.${elemType.simpleName}IteratorWrapper")
  }

  fun mutableKeySetTypeName(elemType: KClass<*>): TypeName {
    return ClassName.bestGuess("org.eclipse.collectionx.set.mutable.Mutable${elemType.simpleName}SetWrapper")
  }

  fun mutableValueCollectionTypeName(elemType: KClass<*>): TypeName {
    return ClassName.bestGuess("org.eclipse.collectionx.collection.mutable.Mutable${elemType.simpleName}CollectionWrapper")
  }

  fun unmodifiableSet(elemType: KClass<*>): TypeName {
    return Types.unmodifiableSets[elemType]!!.asClassName()
  }

  fun unmodifiableCollection(elemType: KClass<*>): TypeName {
    return Types.unmodifiableCollection[elemType]!!.asClassName()
  }

  fun mutableIterator(elemType: KClass<*>): TypeName {
    return Types.mutableIterators[elemType]!!.asClassName()
  }

  // iterator
  val iteratorGenerator = IteratorGenerator()
  Types.iteratorTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = iteratorGenerator.generate(elemTyp, collectionType)
    FileSpec.builder("org.eclipse.collectionx.iterator", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // immutable collection
  val immutableCollectionGenerator = ImmutableCollectionGenerator()
  Types.immutableCollectionTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = immutableCollectionGenerator.generate(elemTyp, collectionType, iteratorTypeName(elemTyp))
    FileSpec.builder("org.eclipse.collectionx.collection.immutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // immutable list
  val immutableListGenerator = ImmutableListGenerator()
  Types.listTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = immutableListGenerator.generate(elemTyp, collectionType)
    FileSpec.builder("org.eclipse.collectionx.list.immutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // immutable set
  val immutableSetGenerator = ImmutableSetGenerator()
  Types.setTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = immutableSetGenerator.generate(elemTyp, collectionType, iteratorTypeName(elemTyp))
    FileSpec.builder("org.eclipse.collectionx.set.immutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // immutable map
  val immutableMapGenerator = ImmutableMapGenerator()
  Types.mapTypes.forEach { (keyType, valueType, collectionType) ->
    val keySetTypeName = mutableKeySetTypeName(keyType)
    val valueCollectionTypeName = mutableValueCollectionTypeName(valueType)
    val unmodifiableSet = unmodifiableSet(keyType)
    val unmodifiableCollection = unmodifiableCollection(valueType)
    val typeSpec = immutableMapGenerator.generate(
      keyType,
      valueType,
      collectionType,
      keySetTypeName,
      valueCollectionTypeName,
      unmodifiableSet,
      unmodifiableCollection
    )
    FileSpec.builder("org.eclipse.collectionx.map.immutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // mutable collection
  val mutableCollectionGenerator = MutableCollectionGenerator()
  Types.mutableCollectionTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = mutableCollectionGenerator.generate(elemTyp, collectionType, mutableIterator(elemTyp))
    FileSpec.builder("org.eclipse.collectionx.collection.mutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // mutable list
  val mutableListGenerator = MutableListGenerator()
  Types.mutableListTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = mutableListGenerator.generate(elemTyp, collectionType)
    FileSpec.builder("org.eclipse.collectionx.list.mutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // mutable set
  val mutableSetGenerator = MutableSetGenerator()
  Types.mutableSetTypes.forEach { (elemTyp, collectionType) ->
    val typeSpec = mutableSetGenerator.generate(elemTyp, collectionType, mutableIterator(elemTyp))
    FileSpec.builder("org.eclipse.collectionx.set.mutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }
  // mutable map
  val mutableMapGenerator = MutableMapGenerator()
  Types.mutableMapTypes.forEach { (keyType, valueType, collectionType) ->
    val keySetTypeName = mutableKeySetTypeName(keyType)
    val valueCollectionTypeName = mutableValueCollectionTypeName(valueType)
    val typeSpec =
      mutableMapGenerator.generate(keyType, valueType, collectionType, keySetTypeName, valueCollectionTypeName)
    FileSpec.builder("org.eclipse.collectionx.map.mutable", typeSpec.name!!)
      .addType(typeSpec)
      .build()
      .writeTo(File(dir))
  }

  // adaptor
  val adapterGenerator = AdapterGenerator()
  adapterGenerator.generate("org.eclipse.collectionx", "Adaptor").writeTo(File(dir))

  // factory extension
  val factoryExtensionGenerator = FactoryExtensionGenerator()
  factoryExtensionGenerator.generate("org.eclipse.collectionx", "FactoryExtension").writeTo(File(dir))
}
