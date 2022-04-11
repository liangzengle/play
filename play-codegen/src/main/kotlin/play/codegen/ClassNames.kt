package play.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

internal fun ClassName.toNullable(): TypeName = this.copy(nullable = true)

internal val JavaDuration = ClassName.bestGuess("java.time.Duration")

internal val Scheduler = ClassName.bestGuess("play.scheduling.Scheduler")

internal val Component = ClassName.bestGuess("org.springframework.stereotype.Component")
internal val Autowired = ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired")

internal val Inject = ClassName.bestGuess("javax.inject.Inject")
internal val Singleton = ClassName.bestGuess("javax.inject.Singleton")
internal val Named = ClassName.bestGuess("javax.inject.Named")

internal val Controller = ClassName.bestGuess("play.mvc.Controller")
internal val AbstractController = ClassName.bestGuess("play.mvc.AbstractController")
internal val Request = ClassName.bestGuess("play.mvc.Request")
internal val RequestResult = ClassName.bestGuess("play.mvc.RequestResult")
internal val PlayerRequest = ClassName.bestGuess("play.mvc.PlayerRequest")
internal val AbstractPlayerRequest = ClassName.bestGuess("play.mvc.AbstractPlayerRequest")
internal val GeneratePlayerRequestMessage = ClassName.bestGuess("play.mvc.GeneratePlayerRequestMessage")
internal val RequestCommander = ClassName.bestGuess("play.mvc.RequestCommander")

// internal val NotPlayerThread = ClassName.bestGuess("play.mvc.NotPlayerThread")
internal val Cmd = ClassName.bestGuess("play.mvc.Cmd")

internal val MsgId = ClassName.bestGuess("play.mvc.MsgId")

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
internal val MultiEntityCacheKey = ClassName.bestGuess("play.entity.cache.MultiEntityCacheKey")
internal val MultiEntityCache = ClassName.bestGuess("play.entity.cache.MultiEntityCache")
internal val MultiEntityCacheInt = ClassName.bestGuess("play.entity.cache.MultiEntityCacheInt")
internal val MultiEntityCacheLong = ClassName.bestGuess("play.entity.cache.MultiEntityCacheLong")
internal val EntityCacheLoader = ClassName.bestGuess("play.entity.cache.EntityCacheLoader")

internal val AbstractResource = ClassName.bestGuess("play.res.AbstractResource")
internal val AbstractConfig = ClassName.bestGuess("play.res.AbstractConfig")
internal val Ignore = ClassName.bestGuess("play.res.Ignore")
internal val UniqueKey = ClassName.bestGuess("play.res.UniqueKey")
internal val ComparableUniqueKey = ClassName.bestGuess("play.res.ComparableUniqueKey")
internal val Grouped = ClassName.bestGuess("play.res.Grouped")
internal val GroupedWithUniqueKey = ClassName.bestGuess("play.res.GroupedWithUniqueKey")
internal val ExtensionKey = ClassName.bestGuess("play.res.ExtensionKey")
internal val DelegatingResourceSet = ClassName.bestGuess("play.res.DelegatingResourceSet")
internal val ResourceSet = ClassName.bestGuess("play.res.ResourceSet")
internal val UniqueKeyResourceSet = ClassName.bestGuess("play.res.UniqueKeyResourceSet")
internal val GroupedResourceSet = ClassName.bestGuess("play.res.GroupedResourceSet")
internal val ResourceGroup = ClassName.bestGuess("play.res.ResourceGroup")
internal val UniqueKeyResourceGroup = ClassName.bestGuess("play.res.UniqueKeyResourceGroup")
internal val ExtensionResourceSet = ClassName.bestGuess("play.res.ExtensionResourceSet")
internal val SingletonResourceSet = ClassName.bestGuess("play.res.SingletonResourceSet")
internal val SingletonResource = ClassName.bestGuess("play.res.SingletonResource")

internal val classOf = MemberName("play.util", "classOf")

internal val Result2 = ClassName.bestGuess("play.util.control.Result2")

internal val IdEnum = ClassName.bestGuess("play.util.enumeration.IdEnum")

internal val IntIntMaps = ClassName.bestGuess("org.eclipse.collections.impl.factory.primitive.IntIntMaps")
internal val IntIntMap = ClassName.bestGuess("org.eclipse.collections.impl.factory.primitive.IntIntMap")

