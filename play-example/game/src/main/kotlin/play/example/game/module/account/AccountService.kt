package play.example.game.module.account

import play.example.game.module.account.entity.Account
import play.example.game.module.account.entity.AccountEntityCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
class AccountService @Inject constructor(private val accountCache: AccountEntityCache) {

  fun getAccount(id: Long): Account {
    return accountCache.getOrThrow(id)
  }
}
