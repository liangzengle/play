package play.example.game.container.gm

import play.example.game.container.gm.GmCommandModule.Cmd
import play.inject.PlayInjector
import play.util.collection.toImmutableMap
import play.util.reflect.ClassScanner
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class GmCommandInvokerManager constructor(
  private val commanderType: Class<*>,
  private val classScanner: ClassScanner,
  injector: PlayInjector
) {

  private val descriptors: Map<String, List<GmCommandDescriptor>> by lazy {
    injector.getInstancesOfType(GmCommandModule::class.java)
      .associate { module -> module.name to getCommandDescriptors(module.javaClass) }
  }

  private val invokerMap: Map<Class<*>, Map<String, GmCommandInvoker>> by lazy {
    classScanner.getOrdinarySubclasses(GmCommandModule::class.java)
      .asSequence().map { moduleClass ->
        moduleClass to moduleClass.declaredMethods.asSequence()
          .filter { it.isAnnotationPresent(Cmd::class.java) }
          .map { method ->
            getCommandName(method) to GmCommandInvoker(method, commanderType)
          }.toImmutableMap()
      }.toImmutableMap()
  }

  fun getInvoker(moduleClass: Class<*>, cmd: String): GmCommandInvoker? {
    return invokerMap[moduleClass]?.get(cmd)
  }

  fun getCommandDescriptors(): Map<String, List<GmCommandDescriptor>> {
    return descriptors
  }

  private fun getCommandName(method: Method): String {
    var name = method.getAnnotation(Cmd::class.java)?.name
    if (name.isNullOrEmpty()) {
      name = method.name
    }
    return name!!
  }

  private fun getCommandDescriptors(clazz: Class<*>): List<GmCommandDescriptor> {
    return clazz.declaredMethods.asSequence()
      .filter { it.isAnnotationPresent(Cmd::class.java) }
      .map(::toDescriptor)
      .toList()
  }

  private fun toDescriptor(method: Method): GmCommandDescriptor {
    val args = method.parameters.asSequence().drop(1).map(::toDescriptor).toList()
    val desc = method.getAnnotation(Cmd::class.java)?.desc ?: ""
    return GmCommandDescriptor(getCommandName(method), desc, args)
  }

  private fun toDescriptor(parameter: Parameter): GmCommandArgDescriptor {
    val arg = parameter.getAnnotation(GmCommandModule.Arg::class.java)
    val name = parameter.name
    val desc = arg?.desc ?: ""
    val defaultValue = arg?.defaultValue ?: ""
    return GmCommandArgDescriptor(name, desc, defaultValue)
  }
}
