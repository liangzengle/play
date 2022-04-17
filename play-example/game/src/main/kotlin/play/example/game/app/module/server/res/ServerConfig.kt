package play.example.game.app.module.server.res

class ServerConfig constructor(val serverId: Short, val platformIds: List<Byte>) {

  override fun toString(): String {
    return "ServerConfig(serverId=$serverId, platformIds=$platformIds)"
  }
}
