package play.example.rpc.api

import io.reactivex.rxjava3.core.Maybe

/**
 *
 * @author LiangZengle
 */
interface RpcPlayerService {

  fun getPlayerName(playerId: Long): Maybe<String>
}
