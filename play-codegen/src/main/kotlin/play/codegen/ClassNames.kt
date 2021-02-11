package play.codegen

import com.squareup.kotlinpoet.ClassName

internal val Inject = ClassName.bestGuess("javax.inject.Inject")
internal val Singleton = ClassName.bestGuess("javax.inject.Singleton")

internal val GuiceModule = ClassName.bestGuess("play.inject.guice.GuiceModule")

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

internal val DisableCodegen = ClassName.bestGuess("play.codegen.DisableCodegen")

internal val Entity = ClassName.bestGuess("play.db.Entity")
internal val EntityInt = ClassName.bestGuess("play.db.EntityInt")
internal val EntityLong = ClassName.bestGuess("play.db.EntityLong")
internal val EntityString = ClassName.bestGuess("play.db.EntityString")
internal val CacheSpec = ClassName.bestGuess("play.db.cache.CacheSpec")
internal val EntityCacheManager = ClassName.bestGuess("play.db.cache.EntityCacheManager")
internal val EntityCache = ClassName.bestGuess("play.db.cache.EntityCache")
internal val EntityCacheInt = ClassName.bestGuess("play.db.cache.EntityCacheInt")
internal val EntityCacheLong = ClassName.bestGuess("play.db.cache.EntityCacheLong")

internal val AbstractConfig = ClassName.bestGuess("play.config.AbstractConfig")
internal val Ignore = ClassName.bestGuess("play.config.Ignore")
internal val UniqueKey = ClassName.bestGuess("play.config.UniqueKey")
internal val ComparableUniqueKey = ClassName.bestGuess("play.config.ComparableUniqueKey")
internal val Grouped = ClassName.bestGuess("play.config.Grouped")
internal val ExtensionKey = ClassName.bestGuess("play.config.ExtensionKey")
internal val DelegatedConfigSet = ClassName.bestGuess("play.config.DelegatedConfigSet")
internal val BasicConfigSet = ClassName.bestGuess("play.config.BasicConfigSet")
internal val UniqueKeyConfigSet = ClassName.bestGuess("play.config.UniqueKeyConfigSet")
internal val GroupedConfigSet = ClassName.bestGuess("play.config.GroupedConfigSet")
internal val ExtensionConfigSet = ClassName.bestGuess("play.config.ExtensionConfigSet")
internal val SingletonConfigSet = ClassName.bestGuess("play.config.SingletonConfigSet")
internal val SingletonConfig = ClassName.bestGuess("play.config.SingletonConfig")
internal val Resource = ClassName.bestGuess("play.config.Resource")

internal val UnsafeEntityCacheOps = ClassName.bestGuess("play.db.cache.UnsafeEntityCacheOps")
