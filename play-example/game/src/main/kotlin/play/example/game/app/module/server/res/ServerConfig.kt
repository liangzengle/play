package play.example.game.app.module.server.res

import play.example.common.id.GameUIDGenerator
import java.util.*

class ServerConfig constructor(val serverId: Short, val platformIds: List<Byte>) {

  fun newIdGenerator(): GameUIDGenerator =
    GameUIDGenerator(platformIds[0].toInt(), serverId.toInt(), OptionalLong.empty())

  override fun toString(): String {
    return "ServerConfig(serverId=$serverId, platformIds=$platformIds)"
  }
}
