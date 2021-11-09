package play.example.game.container.command

import mu.KLogging
import play.inject.PlayInjector
import play.util.control.Result2

class CommandService constructor(
  private val injector: PlayInjector,
  private val commandManager: CommandManager
) {
  companion object : KLogging()

  fun invoke(commander: Any, module: String, cmd: String, args: List<String>): CommandResult {
    val invoker = commandManager.getInvoker(module, cmd) ?: return CommandResult.err("GM指令不存在: $module $cmd")
    val moduleInstance = injector.getInstance(invoker.targetClass)
    return try {
      val result = invoker.invoke(commander, moduleInstance, args)
      onResult(result)
    } catch (e: Exception) {
      logger.debug(e) { "GM指令错误: $invoker $args" }
      CommandResult.err("操作失败, 服务器异常: ${e.javaClass.name}: ${e.message}")
    }
  }

  private fun onResult(result: Any?): CommandResult {
    return when (result) {
      null -> CommandResult.ok("操作成功")
      is CommandResult -> result
      is String -> CommandResult.ok(result)
      is Result2.Err -> CommandResult.err("操作失败: ${result.code}")
      else -> CommandResult.ok(result.toString())
    }
  }

  fun listCommandModules() = commandManager.getCommandDescriptors()
}
