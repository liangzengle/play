package play.example.game.app.module.platform

import play.example.game.app.module.account.domain.AccountId
import play.example.game.app.module.account.domain.DefaultAccountId
import play.example.game.app.module.account.entity.Account
import play.example.game.app.module.platform.domain.Platform
import play.example.game.app.module.platform.domain.Platforms
import play.example.module.login.message.LoginParams
import play.util.time.Time.currentMillis

abstract class PlatformService {

  abstract val platform: Platform

  open fun toAccountId(account: Account): AccountId =
    DefaultAccountId(account.platformId, account.serverId, account.name)

  open fun getAccountId(params: LoginParams) =
    DefaultAccountId(Platforms.getOrThrow(params.platform).id.toByte(), params.serverId.toShort(), params.account)

  open fun newAccount(id: Long, platformId: Byte, serverId: Short, name: String, params: LoginParams): Account =
    Account(id, name, platformId, serverId, currentMillis())

  abstract fun validateLoginParams(params: LoginParams): Int
}
