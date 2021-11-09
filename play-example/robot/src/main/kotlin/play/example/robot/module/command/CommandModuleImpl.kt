package play.example.robot.module.command

import org.springframework.stereotype.Component
import play.example.game.app.module.command.message.CommandModuleList
import play.example.game.container.command.CommandResult
import play.example.robot.module.CommandModule
import play.example.robot.module.player.RobotPlayer

/**
 *
 * @author LiangZengle
 */
@Component
class CommandModuleImpl : CommandModule() {
  override fun listResp(player: RobotPlayer, data: CommandModuleList, req: Any?) {
    for (commandModule in data.commandModules) {
      println(commandModule.label)
      for (command in commandModule.commands) {
        println("\t$command")
      }
    }
  }

  override fun useResp(player: RobotPlayer, data: CommandResult, req: UseRequestParams?) {
  }
}
