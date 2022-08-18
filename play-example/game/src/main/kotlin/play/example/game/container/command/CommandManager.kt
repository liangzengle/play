package play.example.game.container.command

import org.springframework.stereotype.Component
import play.util.collection.toImmutableList
import play.util.collection.toImmutableMap
import play.util.reflect.ClassScanner
import java.lang.reflect.Method
import java.lang.reflect.Parameter

@Component
class CommandManager constructor(private val classScanner: ClassScanner) {

  private lateinit var descriptors: List<CommandModuleDescriptor>

  private lateinit var invokers: Map<String, Map<String, CommandInvoker>>

  @Volatile
  private var initialized = false

  private fun ensureInitialized() {
    if (initialized) {
      return
    }
    synchronized(this) {
      if (initialized) {
        return
      }
      val commandModuleClasses = classScanner.getInstantiatableClassesAnnotatedWith(CommandModule::class.java)
      val descriptors = commandModuleClasses
        .asSequence()
        .map(::toCommandModuleDescriptor)
        .toImmutableList()
      val invokers = commandModuleClasses
        .asSequence()
        .map { moduleClass ->
          val commandModule = moduleClass.getAnnotation(CommandModule::class.java)
          commandModule.name to moduleClass.declaredMethods.asSequence()
            .filter { it.isAnnotationPresent(Command::class.java) }
            .map { method -> getCommandName(method) to CommandInvoker(method) }
            .toImmutableMap()
        }.toImmutableMap()

      this.descriptors = descriptors
      this.invokers = invokers
      this.initialized = true
    }
  }

  fun getInvoker(module: String, cmd: String): CommandInvoker? {
    ensureInitialized()
    return invokers[module]?.get(cmd)
  }

  fun getCommandModuleDescriptors(): List<CommandModuleDescriptor> {
    ensureInitialized()
    return descriptors
  }

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

  private fun toDescriptor(parameter: Parameter): CommandParamDescriptor {
    val param = parameter.getAnnotation(Param::class.java)
    val name = parameter.name
    val desc = param?.desc ?: ""
    val defaultValue = param?.defaultValue ?: ""
    return CommandParamDescriptor(name, desc, defaultValue)
  }
}
