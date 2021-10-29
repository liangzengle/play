package play.example.game.container.gm

import mu.KLogging
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.control.Result2

class GmCommandService constructor(
  private val injector: PlayInjector,
  private val invokerManager: GmCommandInvokerManager
) {
  companion object : KLogging()

  private val moduleMap: Map<String, Class<GmCommandModule>> by lazy {
    injector.getInstancesOfType(GmCommandModule::class.java)
      .asSequence()
      .map { it.name to it.javaClass }
      .toImmutableMap()
  }

  fun invoke(commander: Any, module: String, cmd: String, args: List<String>): GmCommandResult {
    val moduleClass = moduleMap[module] ?: return GmCommandResult.err("GM指令不存在: $module $cmd")
    val invoker = invokerManager.getInvoker(moduleClass, cmd) ?: return GmCommandResult.err("GM指令不存在: $module $cmd")
    val moduleInstance = injector.getInstance(moduleClass)
    return try {
      val result = invoker.invoke(commander, moduleInstance, args)
      onResult(result)
    } catch (e: Exception) {
      logger.debug(e) { "GM指令错误: $invoker $args" }
      GmCommandResult.err("操作失败, 服务器异常: ${e.javaClass.name}: ${e.message}")
    }
  }

  private fun onResult(result: Any?): GmCommandResult {
    return when (result) {
      null -> GmCommandResult.ok("操作成功")
      is GmCommandResult -> result
      is String -> GmCommandResult.ok(result)
      is Result2.Err -> GmCommandResult.err("操作失败: ${result.code}")
      else -> GmCommandResult.ok(result.toString())
    }
  }
}
