package play.example.game.app.module.account

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntLongMaps
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.db.QueryService
import play.example.common.id.GameUIDGenerator
import play.example.game.app.module.account.domain.AccountId
import play.example.game.app.module.account.entity.Account
import play.example.game.app.module.platform.PlatformServiceProvider
import play.util.collection.ConcurrentObjectLongMap
import play.util.max
import play.util.primitive.high16
import play.util.primitive.low16
import play.util.primitive.toInt
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Component
class AccountIdCache @Autowired constructor(
  queryService: QueryService,
  platformServiceProvider: PlatformServiceProvider
) {
  private val accountIdToId: ConcurrentObjectLongMap<AccountId>
  private val idGenerators: MutableIntObjectMap<GameUIDGenerator>

  init {
    accountIdToId = queryService.fold(Account::class.java, ConcurrentObjectLongMap<AccountId>()) { map, account ->
      val platformService = platformServiceProvider.getService(account.platformId.toInt())
      val accountId = platformService.toAccountId(account)
      map[accountId] = account.id
      map
    }.blockingGet(5.seconds)

    val maxIds = IntLongMaps.mutable.empty()
    for ((accountId, id) in accountIdToId) {
      val key = toInt(accountId.platformId.toShort(), accountId.serverId)
      maxIds.updateValue(key, id) { it max id }
    }

    val idGeneratorMap = IntObjectHashMap<GameUIDGenerator>(maxIds.keyValuesView().size())
    for (e in maxIds.keyValuesView()) {
      val key = e.one
      val value = GameUIDGenerator(e.one.high16().toInt(), e.one.low16().toInt(), OptionalLong.of(e.two))
      idGeneratorMap.put(key, value)
    }
    idGenerators = idGeneratorMap
  }

  fun nextId(platformId: Byte, serverId: Short): OptionalLong {
    val key = toInt(platformId.toShort(), serverId)
    val idGenerator = idGenerators.getIfAbsentPut(key) {
      GameUIDGenerator(
        platformId.toInt(),
        serverId.toInt(),
        OptionalLong.empty()
      )
    }
    return idGenerator.next()
  }

  fun getOrNull(accountId: AccountId): Long? {
    return accountIdToId[accountId]
  }

  fun add(accountId: AccountId, id: Long) {
    val prev = accountIdToId.putIfAbsent(accountId, id)
    if (prev != null) {
      throw IllegalStateException("accountId=$accountId id=$id prev=$prev")
    }
  }

  fun size() = accountIdToId.size
}
