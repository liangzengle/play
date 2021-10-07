package play.example.game.container.gm

import mu.KLogging
import play.example.game.container.gm.GmCommandModule.Arg
import play.example.game.container.gm.GmCommandModule.Cmd
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.control.Result2
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class GmCommandService<Commander : Any> constructor(
  private val commanderType: Class<Commander>,
  private val injector: PlayInjector,
) {
  companion object : KLogging()

  private val invokers: Map<String, Map<String, GmCommandInvoker>> by lazy {
    injector.getInstancesOfType(GmCommandModule::class.java).asSequence().map { module ->
      module.name to module.javaClass.declaredMethods.asSequence()
        .filter { it.isAnnotationPresent(Cmd::class.java) }
        .map { method ->
          getCommandName(method) to GmCommandInvoker(method, module, commanderType)
        }.toImmutableMap()
    }.toImmutableMap()
  }

  private val descriptors: Map<String, List<GmCommandDescriptor>> by lazy {
    injector.getInstancesOfType(GmCommandModule::class.java).associate { module ->
      module.name to module.javaClass.declaredMethods.asSequence()
        .filter { it.isAnnotationPresent(Cmd::class.java) }
        .map(::toDescriptor)
        .toList()
    }
  }

  private fun getCommandName(method: Method): String {
    var name = method.getAnnotation(Cmd::class.java)?.name
    if (name.isNullOrEmpty()) {
      name = method.name
    }
    return name!!
  }

  private fun toDescriptor(method: Method): GmCommandDescriptor {
    val args = method.parameters.asSequence().drop(1).map(::toDescriptor).toList()
    val desc = method.getAnnotation(Cmd::class.java)?.desc ?: ""
    return GmCommandDescriptor(getCommandName(method), desc, args)
  }

  private fun toDescriptor(parameter: Parameter): GmCommandArgDescriptor {
    val arg = parameter.getAnnotation(Arg::class.java)
    val name = parameter.name
    val desc = arg?.desc ?: ""
    val defaultValue = arg?.defaultValue ?: ""
    return GmCommandArgDescriptor(name, desc, defaultValue)
  }

  fun invoke(commander: Commander, module: String, cmd: String, args: List<String>): GmCommandResult {
    val invoker = invokers[module]?.get(cmd) ?: return GmCommandResult.err("GM指令不存在: $cmd")
    return try {
      val result = invoker.invoke(commander, args)
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
