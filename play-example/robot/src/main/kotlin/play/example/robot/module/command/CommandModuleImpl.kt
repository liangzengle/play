package play.example.robot.module.command

import org.springframework.stereotype.Component
import play.example.game.container.command.CommandResult
import play.example.module.command.message.CommandModuleListProto
import play.example.robot.module.CommandModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class CommandModuleImpl : CommandModule() {
  override fun listResp(player: RobotPlayer, data: CommandModuleListProto, req: Any?) {
    for (commandModule in data.modules) {
      println(commandModule.label)
      for (command in commandModule.commands) {
        println("\t$command")
      }
    }
  }

  override fun useResp(player: RobotPlayer, data: CommandResult, req: UseRequestParams?) {
  }
}
