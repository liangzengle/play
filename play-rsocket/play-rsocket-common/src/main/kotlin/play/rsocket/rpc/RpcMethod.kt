package play.rsocket.rpc


/**
 * 用于给rpc接口中的方法指定一个id。如未指定，如不指定，则通过方法名hash自动分配
 *
 * @property value Int
 * @constructor
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RpcMethod(val value: Int = 0)
