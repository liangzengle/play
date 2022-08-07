package play.example.game.app.module.command.message

import play.example.game.container.command.CommandModuleDescriptor

/**
 * gm指令模块咧白哦
 * @property commandModules 指令模块列表
 * @property mostUsed 常用指令
 * @constructor
 */
data class CommandModuleList(val commandModules: List<CommandModuleDescriptor>, val mostUsed: CommandModuleDescriptor)
