package play.example.common.gm

import play.example.module.player.Self
import play.inject.Injector
import play.util.control.Result2
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmCommandService @Inject constructor(private val injector: Injector) {
  private val invokers: Map<String, Map<String, GmCommandInvoker>> by lazy {
    injector.instancesOf(GmCommandModule::class.java).map { module ->
      module.name to module.javaClass.declaredMethods.asSequence()
        .filter { it.isAnnotationPresent(GmCommand::class.java) }
        .map { method ->
          getCommandName(method) to GmCommandInvoker(method, module)
        }.toMap()
    }.toMap()
  }

  private val descriptors: Map<String, List<GmCommandDescriptor>> by lazy {
    injector.instancesOf(GmCommandModule::class.java).map { module ->
      module.name to module.javaClass.declaredMethods.asSequence()
        .filter { it.isAnnotationPresent(GmCommand::class.java) }
        .map(::toDescriptor)
        .toList()
    }.toMap()
  }

  private fun getCommandName(method: Method): String {
    var name = method.getAnnotation(GmCommand::class.java)?.name
    if (name.isNullOrEmpty()) {
      name = method.name
    }
    return name!!
  }

  private fun toDescriptor(method: Method): GmCommandDescriptor {
    val args = method.parameters.asSequence().drop(1).map(::toDescriptor).toList()
    val desc = method.getAnnotation(GmCommand::class.java)?.desc ?: "未知"
    return GmCommandDescriptor(getCommandName(method), desc, args)
  }

  private fun toDescriptor(parameter: Parameter): GmCommandArgDescriptor {
    val arg = parameter.getAnnotation(GmCommandArg::class.java)
    val name = parameter.name
    val desc = arg?.desc ?: "未知"
    val defaultValue = arg?.defaultValue ?: ""
    return GmCommandArgDescriptor(name, desc, defaultValue)
  }

  fun invoke(self: Self, module: String, cmd: String, args: List<String>): GmCommandResult {
    val invoker = invokers[module]?.get(cmd) ?: return GmCommandResult.err("GM指令不存在: $cmd")
    return try {
      val result = invoker.invoke(self, args)
      onResult(result)
    } catch (e: Exception) {
      GmCommandResult.err(e.message ?: "操作失败")
    }
  }

  private fun onResult(result: Any?): GmCommandResult {
    return when (result) {
      null -> GmCommandResult.ok("操作成功")
      is GmCommandResult -> result
      is String -> GmCommandResult.err("操作成功")
      is Result2.Err -> GmCommandResult.err("操作失败: ${result.code}")
      else -> GmCommandResult.ok(result.toString())
    }
  }
}
