package play.mvc

import kotlin.reflect.KClass

/**
 * 为请求生成对应的消息类
 *
 * @property interfaceType 消息类需要实现的接口
 * @constructor
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class GeneratePlayerRequestMessage(val interfaceType: KClass<*>)
