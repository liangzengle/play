package play.example.game.app.module.command.message

import play.example.game.container.command.CommandDescriptor
import play.example.game.container.command.CommandModuleDescriptor
import play.example.game.container.command.CommandParamDescriptor
import play.example.module.command.message.CommandModuleListProto
import play.example.module.command.message.CommandModuleProto
import play.example.module.command.message.CommandParamProto
import play.example.module.command.message.CommandProto

fun CommandParamDescriptor.toProto() = CommandParamProto(name, desc, defaultValue)
fun CommandDescriptor.toProto() = CommandProto(module, name, desc, params.map { it.toProto() })
fun CommandModuleDescriptor.toProto() = CommandModuleProto(name, label, commands.map { it.toProto() })
fun CommandModuleList.toProto() = CommandModuleListProto(commandModules.map { it.toProto() }, mostUsed.toProto())
