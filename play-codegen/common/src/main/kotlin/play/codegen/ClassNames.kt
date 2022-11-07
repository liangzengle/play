package play.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

fun ClassName.toNullable(): TypeName = this.copy(nullable = true)

val Flux = ClassName.bestGuess("reactor.core.publisher.Flux")
val Publisher = ClassName.bestGuess("org.reactivestreams.Publisher")

val Payload = ClassName.bestGuess("io.rsocket.Payload")

val JavaDuration = ClassName.bestGuess("java.time.Duration")

val Scheduler = ClassName.bestGuess("play.scheduling.Scheduler")

val Component = ClassName.bestGuess("org.springframework.stereotype.Component")
val Autowired = ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired")

val Inject = ClassName.bestGuess("javax.inject.Inject")
val Singleton = ClassName.bestGuess("javax.inject.Singleton")
val Named = ClassName.bestGuess("javax.inject.Named")

val JakartaInject = ClassName.bestGuess("jakarta.inject.Inject")
val JakartaSingleton = ClassName.bestGuess("jakarta.inject.Singleton")
val JakartaNamed = ClassName.bestGuess("jakarta.inject.Named")


val Controller = ClassName.bestGuess("play.mvc.Controller")
val AbstractController = ClassName.bestGuess("play.mvc.AbstractController")
val Request = ClassName.bestGuess("play.mvc.Request")
val RequestResult = ClassName.bestGuess("play.mvc.RequestResult")
val PlayerRequest = ClassName.bestGuess("play.mvc.PlayerRequest")
val AbstractPlayerRequest = ClassName.bestGuess("play.mvc.AbstractPlayerRequest")
val GeneratePlayerRequestMessage = ClassName.bestGuess("play.mvc.GeneratePlayerRequestMessage")
val RequestCommander = ClassName.bestGuess("play.mvc.RequestCommander")

//  val NotPlayerThread = ClassName.bestGuess("play.mvc.NotPlayerThread")
val Cmd = ClassName.bestGuess("play.mvc.Cmd")

val MsgId = ClassName.bestGuess("play.mvc.MsgId")

val RequestMessage = ClassName.bestGuess("play.mvc.RequestMessage")

val MessageConverter = ClassName.bestGuess("play.mvc.MessageConverter")

val MessageCodec = ClassName.bestGuess("play.mvc.MessageCodec")

val DisableCodegen = ClassName.bestGuess("play.codegen.DisableCodegen")

val Entity = ClassName.bestGuess("play.entity.Entity")
val ObjId = ClassName.bestGuess("play.entity.ObjId")
val EntityInt = ClassName.bestGuess("play.entity.IntIdEntity")
val EntityLong = ClassName.bestGuess("play.entity.LongIdEntity")
val EntityString = ClassName.bestGuess("play.entity.StringIdEntity")
val CacheSpec = ClassName.bestGuess("play.entity.cache.CacheSpec")
val NeverExpireEvaluator = ClassName.bestGuess("play.entity.cache.NeverExpireEvaluator")
val EntityCacheManager = ClassName.bestGuess("play.entity.cache.EntityCacheManager")
val EntityCache = ClassName.bestGuess("play.entity.cache.EntityCache")
val EntityCacheInt = ClassName.bestGuess("play.entity.cache.EntityCacheInt")
val EntityCacheLong = ClassName.bestGuess("play.entity.cache.EntityCacheLong")
val UnsafeEntityCacheOps = ClassName.bestGuess("play.entity.cache.UnsafeEntityCacheOps")
val EntityCacheInternalApi = ClassName.bestGuess("play.entity.cache.EntityCacheInternalApi")
val CacheIndex = ClassName.bestGuess("play.entity.cache.CacheIndex")
val IndexedEntityCache = ClassName.bestGuess("play.entity.cache.IndexedEntityCache")
val DefaultIndexedEntityCache = ClassName.bestGuess("play.entity.cache.DefaultIndexedEntityCache")
val LongLongIndexedEntityCache = ClassName.bestGuess("play.entity.cache.LongLongIndexedEntityCache")
val LongIntIndexedEntityCache = ClassName.bestGuess("play.entity.cache.LongIntIndexedEntityCache")
val ObjectLongIndexedEntityCache = ClassName.bestGuess("play.entity.cache.ObjectLongIndexedEntityCache")
val EntityCacheLoader = ClassName.bestGuess("play.entity.cache.EntityCacheLoader")

val AbstractResource = ClassName.bestGuess("play.res.AbstractResource")
val AbstractConfig = ClassName.bestGuess("play.res.AbstractConfig")
val Ignore = ClassName.bestGuess("play.res.Ignore")
val UniqueKey = ClassName.bestGuess("play.res.UniqueKey")
val ComparableUniqueKey = ClassName.bestGuess("play.res.ComparableUniqueKey")
val Grouped = ClassName.bestGuess("play.res.Grouped")
val GroupedWithUniqueKey = ClassName.bestGuess("play.res.GroupedWithUniqueKey")
val ExtensionKey = ClassName.bestGuess("play.res.ExtensionKey")
val DelegatingResourceSet = ClassName.bestGuess("play.res.DelegatingResourceSet")
val ResourceSet = ClassName.bestGuess("play.res.ResourceSet")
val UniqueKeyResourceSet = ClassName.bestGuess("play.res.UniqueKeyResourceSet")
val GroupedResourceSet = ClassName.bestGuess("play.res.GroupedResourceSet")
val ResourceGroup = ClassName.bestGuess("play.res.ResourceGroup")
val UniqueKeyResourceGroup = ClassName.bestGuess("play.res.UniqueKeyResourceGroup")
val ExtensionResourceSet = ClassName.bestGuess("play.res.ExtensionResourceSet")
val SingletonResourceSet = ClassName.bestGuess("play.res.SingletonResourceSet")
val SingletonResource = ClassName.bestGuess("play.res.SingletonResource")

val classOf = MemberName("play.util", "classOf")

val Result2 = ClassName.bestGuess("play.util.control.Result2")

val IdEnum = ClassName.bestGuess("play.util.enumeration.IdEnum")

val IntIntMaps = ClassName.bestGuess("org.eclipse.collections.impl.factory.primitive.IntIntMaps")
val IntIntMap = ClassName.bestGuess("org.eclipse.collections.impl.factory.primitive.IntIntMap")

val typeOf = MemberName("kotlin.reflect", "typeOf")
val javaType = MemberName("kotlin.reflect.jvm", "javaType")

val ByteBuf = ClassName.bestGuess("io.netty.buffer.ByteBuf")
val ByteBufInputStream = ClassName.bestGuess("io.netty.buffer.ByteBufInputStream")
val ByteBufOutputStream = ClassName.bestGuess("io.netty.buffer.ByteBufOutputStream")
val ByteBufAllocator = ClassName.bestGuess("io.netty.buffer.ByteBufAllocator")

val RpcServiceInterface = ClassName.bestGuess("play.rsocket.rpc.RpcServiceInterface")
val RpcServiceImplementation = ClassName.bestGuess("play.rsocket.rpc.RpcServiceImplementation")
val RpcMethod = ClassName.bestGuess("play.rsocket.rpc.RpcMethod")
val RpcServiceStub = ClassName.bestGuess("play.rsocket.rpc.RpcServiceStub")
val RpcMethodMetadata = ClassName.bestGuess("play.rsocket.rpc.RpcMethodMetadata")

val LocalServiceCaller = ClassName.bestGuess("play.rsocket.rpc.LocalServiceCaller")

//val RemoteServiceStub = ClassName.bestGuess("play.rpc.requester.RemoteServiceStub")
val AbstractRSocketRequester = ClassName.bestGuess("play.rsocket.rpc.AbstractRSocketRequester")

val ByteBufToIOStreamAdapter = ClassName.bestGuess("play.rsocket.serializer.ByteBufToIOStreamAdapter")

//val PlaySerializer = ClassName.bestGuess("play.rsocket.serializer.PlaySerializer")
val RSocketSerializerProvider = ClassName.bestGuess("play.rsocket.serializer.RSocketSerializerProvider")

//val RpcServiceNotFound = MemberName("play.rpc.responder.LocalServiceInvoker", "NotFound")

