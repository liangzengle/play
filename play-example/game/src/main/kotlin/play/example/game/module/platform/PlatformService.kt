package play.example.game.module.platform

import play.example.game.module.account.domain.AccountId
import play.example.game.module.account.domain.DefaultAccountId
import play.example.game.module.account.entity.Account
import play.example.game.module.account.message.LoginParams
import play.example.game.module.platform.domain.Platform
import play.inject.guice.EnableMultiBinding
import play.util.time.currentMillis

@EnableMultiBinding
abstract class PlatformService {

  abstract val platform: Platform

  open fun toAccountId(account: Account): AccountId =
    DefaultAccountId(account.platformId, account.serverId, account.name)

  open fun getAccountId(params: LoginParams) =
    DefaultAccountId(Platform.getOrThrow(params.platform).id.toByte(), params.serverId.toShort(), params.account)

  open fun newAccount(id: Long, platformId: Byte, serverId: Short, name: String, params: LoginParams): Account =
    Account(id, name, platformId, serverId, currentMillis())

  abstract fun validateLoginParams(params: LoginParams): Int
}
