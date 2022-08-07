package play.example.game.app.module.command

import com.google.common.math.IntMath
import org.springframework.stereotype.Component
import play.example.game.app.module.ModuleId
import play.example.game.app.module.command.entity.CommandId
import play.example.game.app.module.command.entity.CommandStatisticsEntityCache
import play.example.game.app.module.command.message.CommandModuleList
import play.example.game.app.module.command.message.toProto
import play.example.game.app.module.player.PlayerManager.Self
import play.example.game.container.command.CommandDescriptor
import play.example.game.container.command.CommandModuleDescriptor
import play.example.game.container.command.CommandResult
import play.example.game.container.command.CommandService
import play.example.module.command.message.CommandModuleListProto
import play.mvc.AbstractController
import play.mvc.Cmd
import play.mvc.Controller
import play.mvc.RequestResult
import play.util.control.ok

/**
 * gm指令
 * @author LiangZengle
 */
@Controller(ModuleId.Command)
@Component
class CommandController(
  private val commandService: CommandService,
  private val entityCache: CommandStatisticsEntityCache
) : AbstractController(ModuleId.Command) {

  @Cmd(1)
  fun use(self: Self, cmd: String): RequestResult<CommandResult> = RequestResult {
    val cmdParts = cmd.split(' ')
    ok {
      if (cmdParts.size < 2) {
        CommandResult.err("GM指令参数错误: $cmd")
      } else {
        val module = cmdParts[0]
        val command = cmdParts[1]
        val args = if (cmdParts.size > 2) cmdParts.subList(2, cmdParts.size) else emptyList()
        val result = commandService.invoke(self, module, command, args)
        increaseUseTimes(module, command)
        result
      }
    }
  }

  @Cmd(2)
  fun list(self: Self): RequestResult<CommandModuleListProto> {
    val commandModules = commandService.getCommandModuleDescriptors()
    val selectMostUsedCommands = selectMostUsedCommands(commandModules, 20)
    val mostUsed = CommandModuleDescriptor("mostUsed", "常用", selectMostUsedCommands)
    return RequestResult.ok(CommandModuleList(commandModules, mostUsed).toProto())
  }

  @Suppress("UnstableApiUsage")
  private fun increaseUseTimes(module: String, cmd: String) {
    val entity = entityCache.getOrCreate(1)
    entity.statistics.merge(CommandId(module, cmd), 1, IntMath::saturatedAdd)
  }

  @Suppress("SameParameterValue")
  private fun selectMostUsedCommands(
    commandModules: List<CommandModuleDescriptor>,
    count: Int
  ): List<CommandDescriptor> {
    val entity = entityCache.getOrCreate(1)
    val topN =
      entity.statistics.entries.asSequence().sortedByDescending { it.value }.map { it.key }.take(count).toList()
    return commandModules.asSequence().flatMap { it.commands }.filter { topN.contains(CommandId(it.module, it.name)) }
      .toList()
  }
}
