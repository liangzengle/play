package play.example.game.app.rpc.provider

import com.alibaba.rsocket.RSocketService
import io.reactivex.rxjava3.core.Maybe
import org.springframework.stereotype.Service
import play.example.game.app.module.player.PlayerService
import play.example.game.container.gs.domain.GameServerId
import play.example.rpc.api.RpcPlayerService

/**
 *
 * @author LiangZengle
 */
@Service
@RSocketService(serviceInterface = RpcPlayerService::class)
class RpcPlayerServiceImpl(private val playerService: PlayerService, private val gameServerId: GameServerId) :
  RpcPlayerService {

  override fun getPlayerName(playerId: Long): Maybe<String> {
    return Maybe.just(playerService.getPlayerNameOrElse(playerId, "$playerId @ $gameServerId"))
  }
}
