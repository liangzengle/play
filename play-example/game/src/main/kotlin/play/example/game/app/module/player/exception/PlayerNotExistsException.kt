package play.example.game.app.module.player.exception

/**
 * 玩家不存在异常
 * @author LiangZengle
 */
class PlayerNotExistsException(playerId: Long, writeStackTrace: Boolean) :
  RuntimeException(playerId.toString(), null, false, writeStackTrace) {
  constructor(playerId: Long) : this(playerId, true)
}
