package play.codegen

import com.squareup.kotlinpoet.ClassName

val Inject = ClassName.bestGuess("javax.inject.Inject")
val Singleton = ClassName.bestGuess("javax.inject.Singleton")

val GuiceModule = ClassName.bestGuess("play.inject.guice.GuiceModule")

val Controller = ClassName.bestGuess("play.mvc.Controller")
val AbstractController = ClassName.bestGuess("play.mvc.AbstractController")
val Request = ClassName.bestGuess("play.mvc.Request")
val RequestResult = ClassName.bestGuess("play.mvc.RequestResult")
val NotPlayerThread = ClassName.bestGuess("play.mvc.NotPlayerThread")
val Cmd = ClassName.bestGuess("play.mvc.Cmd")

val DisableCodegen = ClassName.bestGuess("play.codegen.DisableCodegen")

val Entity = ClassName.bestGuess("play.db.Entity")
val EntityInt = ClassName.bestGuess("play.db.EntityInt")
val EntityLong = ClassName.bestGuess("play.db.EntityLong")
val EntityString = ClassName.bestGuess("play.db.EntityString")
val CacheSpec = ClassName.bestGuess("play.db.cache.CacheSpec")
val EntityCacheManager = ClassName.bestGuess("play.db.cache.EntityCacheManager")
val EntityCache = ClassName.bestGuess("play.db.cache.EntityCache")


val AbstractConfig = ClassName.bestGuess("play.config.AbstractConfig")
val Ignore = ClassName.bestGuess("play.config.Ignore")
val UniqueKey = ClassName.bestGuess("play.config.UniqueKey")
val ComparableUniqueKey = ClassName.bestGuess("play.config.ComparableUniqueKey")
val Grouped = ClassName.bestGuess("play.config.Grouped")
val ExtensionKey = ClassName.bestGuess("play.config.ExtensionKey")
val DelegatedConfigSet = ClassName.bestGuess("play.config.DelegatedConfigSet")
val BasicConfigSet = ClassName.bestGuess("play.config.BasicConfigSet")
val UniqueKeyConfigSet = ClassName.bestGuess("play.config.UniqueKeyConfigSet")
val GroupedConfigSet = ClassName.bestGuess("play.config.GroupedConfigSet")
val ExtensionConfigSet = ClassName.bestGuess("play.config.ExtensionConfigSet")
val SingletonConfigSet = ClassName.bestGuess("play.config.SingletonConfigSet")
val SingletonConfig = ClassName.bestGuess("play.config.SingletonConfig")
val Resource = ClassName.bestGuess("play.config.Resource")
