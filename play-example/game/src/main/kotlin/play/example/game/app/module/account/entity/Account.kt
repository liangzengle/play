package play.example.game.app.module.account.entity

import play.example.game.app.module.player.entity.AbstractPlayerEntity

/**
 * Created by liang on 2020/6/27.
 */
class Account(id: Long, val name: String, val platformId: Byte, val serverId: Short, val ctime: Long) :
  AbstractPlayerEntity(id)
