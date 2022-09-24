package play.example.game.container.command

import mu.KLogging
import play.inject.PlayInjector
import play.util.LambdaClassValue
import play.util.control.Result2

class CommandService(private val beanProvider: (Class<*>) -> Any, private val commandManager: CommandManager) {
  constructor(injector: PlayInjector, commandManager: CommandManager) : this(
    { injector.getInstance(it) },
    commandManager
  )

  companion object : KLogging()

  private val beanCache = LambdaClassValue(beanProvider::invoke)

  fun invoke(commander: Any, module: String, cmd: String, args: List<String>): CommandResult {
    val invoker = commandManager.getInvoker(module, cmd) ?: return CommandResult.err("GM指令不存在: $module $cmd")
    return try {
      val target = beanCache.get(invoker.targetClass)
      val result = invoker.invoke(commander, target, args)
      onResult(result)
    } catch (e: Exception) {
      when (e) {
        is CommandParamMissingException -> CommandResult.err(e.message!!)
        is CommandParamIllegalException -> CommandResult.err(e.message!!)
        else -> {
          logger.debug(e) { "GM指令错误: $invoker $args" }
          CommandResult.err("操作失败, 服务器异常: ${e.javaClass.name}: ${e.message}")
        }
      }
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

  fun getCommandModuleDescriptors() = commandManager.getCommandModuleDescriptors()
}
