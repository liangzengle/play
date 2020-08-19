package play.example.module.platform

import play.example.module.account.domain.AccountId
import play.example.module.account.domain.DefaultAccountId
import play.example.module.account.entity.Account
import play.example.module.account.message.LoginProto
import play.example.module.platform.domain.Platform
import play.util.time.currentMillis

abstract class PlatformService {

  abstract val platform: Platform

  open fun toAccountId(account: Account): AccountId =
    DefaultAccountId(account.platformId, account.serverId, account.name)

  open fun getAccountId(params: LoginProto) =
    DefaultAccountId(Platform.getOrThrow(params.platform).id.toByte(), params.serverId.toShort(), params.account)

  open fun createAccount(id: Long, platformId: Byte, serverId: Short, name: String, params: LoginProto): Account =
    Account(id, name, platformId, serverId, currentMillis())
}
