package play.example.game.container.rpc

import com.alibaba.rsocket.metadata.GSVRoutingMetadata
import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller
import com.alibaba.rsocket.rpc.ReactiveMethodHandler

/**
 *
 * @author LiangZengle
 */
interface GSVLocalReactiveServiceCaller : LocalReactiveServiceCaller {

  fun getInvokeMethod(routing: GSVRoutingMetadata): ReactiveMethodHandler?
}
