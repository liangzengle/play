package play.example.module.player.exception

/**
 * 玩家不存在异常
 * @author LiangZengle
 */
class PlayerNotExistsException(playerId: Long) : RuntimeException(playerId.toString())
