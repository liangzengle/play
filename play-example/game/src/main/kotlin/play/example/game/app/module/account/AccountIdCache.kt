package play.example.game.app.module.account

import mu.KLogging
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap
import org.eclipse.collections.impl.factory.primitive.IntLongMaps
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.db.QueryService
import play.example.game.app.module.account.domain.AccountId
import play.example.game.app.module.account.entity.Account
import play.example.game.app.module.platform.PlatformServiceProvider
import play.util.collection.ConcurrentObjectLongMap
import play.util.max
import play.util.primitive.toInt
import java.time.Duration
import java.util.*

@Component
class AccountIdCache @Autowired constructor(
  queryService: QueryService,
  platformServiceProvider: PlatformServiceProvider
) {
  companion object : KLogging()

  private val accountIdToId: ConcurrentObjectLongMap<AccountId>
  private val idGenerators: MutableIntObjectMap<AccountIdGenerator>

  init {
    accountIdToId = queryService.queryAll(Account::class.java)
      .collect({ ConcurrentObjectLongMap<AccountId>() }, { map, account ->
        val platformService = platformServiceProvider.getService(account.platformId.toInt())
        val accountId = platformService.toAccountId(account)
        map[accountId] = account.id
      }).block(Duration.ofSeconds(5))!!

    val maxIds = IntLongMaps.mutable.empty()
    for ((accountId, id) in accountIdToId) {
      val key = toInt(accountId.platformId.toShort(), accountId.serverId)
      maxIds.updateValue(key, id) { it max id }
    }

    val idGeneratorMap = IntObjectHashMap<AccountIdGenerator>(maxIds.keyValuesView().size())
    for (e in maxIds.keyValuesView()) {
      val key = e.one
      val sequence = AccountIdGenerator.extractSequence(e.two)
      val value = AccountIdGenerator(e.one, sequence)
      idGeneratorMap.put(key, value)
    }
    idGenerators = idGeneratorMap
  }

  fun nextId(platformId: Byte, serverId: Short): OptionalLong {
    val key = toInt(platformId.toShort(), serverId)
    val idGenerator = idGenerators.getIfAbsentPut(key) {
      AccountIdGenerator(toInt(platformId.toShort(), serverId), 0)
    }
    return try {
      val nextId = idGenerator.nextId()
      OptionalLong.of(nextId)
    } catch (e: Exception) {
      logger.error(e) { "Can not generate id" }
      OptionalLong.empty()
    }
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
