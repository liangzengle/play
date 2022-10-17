package play.example.game.app.module.player

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.db.QueryService
import play.example.game.app.module.player.domain.PlayerErrorCode
import play.example.game.app.module.player.entity.PlayerInfoEntity
import play.example.game.app.module.player.exception.PlayerNotExistsException
import play.util.collection.ConcurrentLongObjectMap
import play.util.collection.ConcurrentObjectLongMap
import play.util.control.Result2
import play.util.toOptional
import java.time.Duration
import java.util.*

@Component
class PlayerIdNameCache @Autowired constructor(queryService: QueryService) {
  private val idToName = ConcurrentLongObjectMap<String>()
  private val nameToId = ConcurrentObjectLongMap<String>()

  init {
    queryService.query(PlayerInfoEntity::class.java, listOf("name")).doOnNext { result ->
      val id = result.getLong("id")
      val name = result.getString("name")
      val prevName = idToName.put(id, name)
      val prevId = nameToId.put(name, id)
      check(prevName == null) { "Duplicate id: $id" }
      check(prevId == null) { "Duplicate name: $name" }
    }.blockLast(Duration.ofSeconds(30))
  }

  @JvmRecord
  data class ChangeNameResult(val state: Byte, val oldName: String?) {
    companion object State {
      const val Success: Byte = 0
      const val NewNameSameAsTheOldOne: Byte = 1
      const val PlayerNameNotAvailable: Byte = 2
    }

    fun toResult2(): Result2<Nothing> {
      return when (state) {
        Success -> PlayerErrorCode.Success
        NewNameSameAsTheOldOne -> PlayerErrorCode.NewNameSameAsTheOldOne
        else -> PlayerErrorCode.PlayerNameNotAvailable
      }
    }
  }

  fun changeName(id: Long, newName: String): ChangeNameResult {
    val resultHolder = arrayOfNulls<ChangeNameResult>(1)
    nameToId.compute(newName) { _, v ->
      // 名字可用
      if (v === null) {
        val oldName = idToName.put(id, newName)
        resultHolder[0] = ChangeNameResult(ChangeNameResult.Success, oldName)
        id
      } else {
        if (v == id) {
          resultHolder[0] = ChangeNameResult(ChangeNameResult.NewNameSameAsTheOldOne, null)
        }
        v
      }
    }
    val result = resultHolder[0] ?: ChangeNameResult(ChangeNameResult.PlayerNameNotAvailable, null)
    val oldName = result.oldName
    if (oldName !== null && !nameToId.remove(oldName, id)) {
      throw IllegalStateException("移除旧名字映射失败: id=$id, oldName=$oldName, newName=$newName")
    }
    return result
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
