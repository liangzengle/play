package play.example.game.app.module.player.domain

import play.example.module.login.message.LoginParams

class OnlinePlayer(val playerId: Long, var loginParams: LoginParams, var logoutTime: Long = 0) {


  override fun equals(other: Any?): Boolean {
    return other is OnlinePlayer && this.playerId == other.playerId
  }

  override fun hashCode(): Int {
    return playerId.hashCode()
  }

  override fun toString(): String {
    return "OnlinePlayer($playerId)"
  }
}
