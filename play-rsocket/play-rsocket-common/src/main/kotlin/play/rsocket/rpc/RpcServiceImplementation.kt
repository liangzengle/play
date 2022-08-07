package play.rsocket.rpc

import kotlin.reflect.KClass


/**
 * 用来指定实现的rpc接口
 *
 * @property value 实现的rpc接口
 * @constructor
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RpcServiceImplementation(val value: KClass<*>)
