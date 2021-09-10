package play.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

internal val Inject = ClassName.bestGuess("javax.inject.Inject")
internal val Singleton = ClassName.bestGuess("javax.inject.Singleton")
internal val Named = ClassName.bestGuess("javax.inject.Named")

internal val Provides = ClassName.bestGuess("com.google.inject.Provides")
internal val GoogleInjectModule = ClassName.bestGuess("com.google.inject.Module")
internal val AbstractModule = ClassName.bestGuess("com.google.inject.AbstractModule")

internal val GuiceModule = ClassName.bestGuess("play.inject.guice.GuiceModule")
internal val GeneratedMultiBindModule = ClassName.bestGuess("play.inject.guice.GeneratedMultiBindModule")
internal val EnableMultiBinding = ClassName.bestGuess("play.inject.guice.EnableMultiBinding")
internal val MultiBindListProvider = ClassName.bestGuess("play.inject.guice.MultiBindListProvider")
internal val MultiBindSetProvider = ClassName.bestGuess("play.inject.guice.MultiBindSetProvider")
internal val PlayInjector = ClassName.bestGuess("play.inject.Injector")

internal val Controller = ClassName.bestGuess("play.mvc.Controller")
internal val AbstractController = ClassName.bestGuess("play.mvc.AbstractController")
internal val Request = ClassName.bestGuess("play.mvc.Request")
internal val RequestResult = ClassName.bestGuess("play.mvc.RequestResult")
internal val PlayerRequest = ClassName.bestGuess("play.mvc.PlayerRequest")
internal val AbstractPlayerRequest = ClassName.bestGuess("play.mvc.AbstractPlayerRequest")
internal val GeneratePlayerRequestMessage = ClassName.bestGuess("play.mvc.GeneratePlayerRequestMessage")

// internal val NotPlayerThread = ClassName.bestGuess("play.mvc.NotPlayerThread")
internal val Cmd = ClassName.bestGuess("play.mvc.Cmd")

internal val RequestMessage = ClassName.bestGuess("play.mvc.RequestMessage")

internal val MessageConverter = ClassName.bestGuess("play.mvc.MessageConverter")

internal val MessageCodec = ClassName.bestGuess("play.mvc.MessageCodec")

internal val DisableCodegen = ClassName.bestGuess("play.codegen.DisableCodegen")

internal val Entity = ClassName.bestGuess("play.entity.Entity")
internal val EntityInt = ClassName.bestGuess("play.entity.IntIdEntity")
internal val EntityLong = ClassName.bestGuess("play.entity.LongIdEntity")
internal val CacheSpec = ClassName.bestGuess("play.entity.cache.CacheSpec")
internal val EntityCacheManager = ClassName.bestGuess("play.entity.cache.EntityCacheManager")
internal val EntityCache = ClassName.bestGuess("play.entity.cache.EntityCache")
internal val EntityCacheInt = ClassName.bestGuess("play.entity.cache.EntityCacheInt")
internal val EntityCacheLong = ClassName.bestGuess("play.entity.cache.EntityCacheLong")
internal val UnsafeEntityCacheOps = ClassName.bestGuess("play.entity.cache.UnsafeEntityCacheOps")

internal val AbstractResource = ClassName.bestGuess("play.res.AbstractResource")
internal val AbstractConfig = ClassName.bestGuess("play.res.AbstractConfig")
internal val Ignore = ClassName.bestGuess("play.res.Ignore")
internal val UniqueKey = ClassName.bestGuess("play.res.UniqueKey")
internal val ComparableUniqueKey = ClassName.bestGuess("play.res.ComparableUniqueKey")
internal val Grouped = ClassName.bestGuess("play.res.Grouped")
internal val GroupedUniqueKey = ClassName.bestGuess("play.res.GroupedUniqueKey")
internal val ExtensionKey = ClassName.bestGuess("play.res.ExtensionKey")
internal val DelegatedResourceSet = ClassName.bestGuess("play.res.DelegatedResourceSet")
internal val ResourceSet = ClassName.bestGuess("play.res.ResourceSet")
internal val UniqueKeyResourceSet = ClassName.bestGuess("play.res.UniqueKeyResourceSet")
internal val GroupedResourceSet = ClassName.bestGuess("play.res.GroupedResourceSet")
internal val GroupResourceSet = ClassName.bestGuess("play.res.GroupResourceSet")
internal val GroupUniqueKeyResourceSet = ClassName.bestGuess("play.res.GroupUniqueKeyResourceSet")
internal val ExtensionResourceSet = ClassName.bestGuess("play.res.ExtensionResourceSet")
internal val SingletonResourceSet = ClassName.bestGuess("play.res.SingletonResourceSet")
internal val SingletonResource = ClassName.bestGuess("play.res.SingletonResource")

val classOf = MemberName("play.util", "classOf")
