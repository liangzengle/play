package play.example.game.app.module.account.domain

abstract class AccountId {
  abstract val platformId: Byte
  abstract val serverId: Short
  abstract val name: String
}

data class DefaultAccountId(
  override val platformId: Byte,
  override val serverId: Short,
  override val name: String
) : AccountId()
