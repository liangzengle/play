package play.rsocket.rpc


/**
 * 用于给rpc接口指定一个id，类似模块号。如不指定，则通过类名hash自动分配
 *
 * @property serviceId rpc接口的id
 * @property generateStub 是否为rpc接口生成stub
 * @constructor
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RpcServiceInterface(val serviceId: Int = 0, val generateStub: Boolean = false)
