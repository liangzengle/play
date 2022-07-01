package play.example.game.app.module.player

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.db.QueryService
import play.example.game.app.module.account.AccountIdGenerator
import play.example.game.app.module.player.entity.PlayerInfoEntity
import play.example.game.app.module.player.exception.PlayerNotExistsException
import play.util.collection.ConcurrentIntObjectMap
import play.util.collection.ConcurrentLongObjectMap
import play.util.collection.ConcurrentObjectLongMap
import play.util.toOptional
import java.time.Duration
import java.util.*

@Component
class PlayerIdNameCache @Autowired constructor(queryService: QueryService) {
  private val idToName: ConcurrentLongObjectMap<String>
  private val serverToNameToId: ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>

  init {
    val idToName = ConcurrentLongObjectMap<String>()
    val serverToNameToId = ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>()
    queryService.query(PlayerInfoEntity::class.java, listOf("name")).doOnNext { result ->
      val id = result.getLong("id")
      val name = result.getString("name")
      add(id, name, idToName, serverToNameToId)
    }.blockLast(Duration.ofSeconds(5))
    this.idToName = idToName
    this.serverToNameToId = serverToNameToId
  }

  fun add(playerId: Long, playerName: String) {
    add(playerId, playerName, idToName, serverToNameToId)
  }

  private fun add(
    playerId: Long,
    playerName: String,
    idToName: ConcurrentLongObjectMap<String>,
    serverToNameToId: ConcurrentIntObjectMap<ConcurrentObjectLongMap<String>>
  ) {
    val prevName = idToName.putIfAbsent(playerId, playerName)
    assert(prevName == null)
    val serverId = AccountIdGenerator.extractNodeId(playerId)
    val nameToId = serverToNameToId.computeIfAbsent(serverId) { ConcurrentObjectLongMap(1024) }
    val prevId = nameToId.putIfAbsent(playerName, playerId)
    assert(prevId == null)
  }

  fun isPlayerNameAvailable(id: Long, name: String): Boolean {
    val serverId = AccountIdGenerator.extractNodeId(id)
    return !(serverToNameToId[serverId]?.containsKey(name) ?: false)
  }

  fun isPlayerExists(playerId: Long): Boolean {
    return idToName.containsKey(playerId)
  }

  fun getPlayerNameOrThrow(playerId: Long): String {
    return idToName[playerId] ?: throw PlayerNotExistsException(playerId)
  }

  fun getPlayerName(playerId: Long): Optional<String> {
    return idToName[playerId].toOptional()
  }
}
