package play.rsocket.rpc

/**
 *
 * @author LiangZengle
 */
fun interface RpcClientInterceptor {
  fun apply(rpcClient: RpcClient): RpcClient
}
