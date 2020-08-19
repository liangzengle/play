package play.example.module.account

import play.example.module.account.entity.Account
import play.example.module.account.entity.AccountCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by liang on 2020/6/27.
 */
@Singleton
class AccountService @Inject constructor(private val accountCache: AccountCache) {

  fun getAccount(id: Long): Account {
    return accountCache.getOrThrow(id)
  }
}
