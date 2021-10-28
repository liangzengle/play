package play.example.game.app.module.account

import org.springframework.stereotype.Component
import play.example.game.app.module.account.entity.Account
import play.example.game.app.module.account.entity.AccountEntityCache

/**
 * Created by liang on 2020/6/27.
 */
@Component
class AccountService(private val accountCache: AccountEntityCache) {

  fun getAccount(id: Long): Account {
    return accountCache.getOrThrow(id)
  }
}
