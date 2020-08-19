package play.example.module.account.entity

import play.example.module.player.entity.PlayerEntity

/**
 * Created by liang on 2020/6/27.
 */
class Account(id: Long, val name: String, val platformId: Byte, val serverId: Short, val ctime: Long) : PlayerEntity(id)
