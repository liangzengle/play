package play.rsocket.rpc

import com.alibaba.rsocket.rpc.LocalReactiveServiceCaller
import com.alibaba.rsocket.rpc.ReactiveMethodHandler

/**
 *
 * @author LiangZengle
 */
interface GSVLocalReactiveServiceCaller : LocalReactiveServiceCaller {
  fun getInvokeMethod(group: String?, version: String?, serviceName: String, method: String): ReactiveMethodHandler?
}
