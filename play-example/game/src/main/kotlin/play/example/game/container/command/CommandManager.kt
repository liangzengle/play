package play.example.game.container.command

import play.util.collection.toImmutableList
import play.util.collection.toImmutableMap
import play.util.reflect.ClassScanner
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class CommandManager constructor(
  private val commanderType: Class<*>,
  private val classScanner: ClassScanner
) {

  private val descriptors: List<CommandModuleDescriptor> by lazy {
    classScanner.getOrdinaryClassesAnnotatedWith(CommandModule::class.java)
      .asSequence()
      .map(::toCommandModuleDescriptor)
      .toImmutableList()
  }

  private val invokerMap: Map<String, Map<String, CommandInvoker>> by lazy {
    classScanner.getOrdinaryClassesAnnotatedWith(CommandModule::class.java)
      .asSequence()
      .map { moduleClass ->
        val commandModule = moduleClass.getAnnotation(CommandModule::class.java)
        commandModule.name to moduleClass.declaredMethods.asSequence()
          .filter { it.isAnnotationPresent(Command::class.java) }
          .map { method ->
            getCommandName(method) to CommandInvoker(method, commanderType)
          }.toImmutableMap()
      }.toImmutableMap()
  }

  fun getInvoker(module: String, cmd: String): CommandInvoker? {
    return invokerMap[module]?.get(cmd)
  }

  fun getCommandDescriptors() = descriptors

  private fun getCommandName(method: Method): String {
    val name = method.getAnnotation(Command::class.java)?.name
    return if (name.isNullOrEmpty()) method.name else name
  }

  private fun toCommandModuleDescriptor(clazz: Class<*>): CommandModuleDescriptor {
    val commandModule = clazz.getAnnotation(CommandModule::class.java)
    val commands = clazz.declaredMethods.asSequence()
      .filter { it.isAnnotationPresent(Command::class.java) }
      .map { toDescriptor(commandModule.name, it) }
      .toList()
    return CommandModuleDescriptor(commandModule.name, commandModule.label, commands)
  }

  private fun toDescriptor(module: String, method: Method): CommandDescriptor {
    val args = method.parameters.asSequence().drop(1).map(::toDescriptor).toList()
    val desc = method.getAnnotation(Command::class.java)?.desc ?: ""
    return CommandDescriptor(module, getCommandName(method), desc, args)
  }

  private fun toDescriptor(parameter: Parameter): CommandArgDescriptor {
    val arg = parameter.getAnnotation(Arg::class.java)
    val name = parameter.name
    val desc = arg?.desc ?: ""
    val defaultValue = arg?.defaultValue ?: ""
    return CommandArgDescriptor(name, desc, defaultValue)
  }
}
