package play.example.game.app.module.task.event.adpater

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import play.example.game.App
import play.example.game.app.module.task.event.TaskEvent
import play.util.reflect.Reflect

/**
 *
 * @author LiangZengle
 */
object TaskEventAdapters {

  private object AdapterMapHolder {
    val adapterMap: ListMultimap<Class<*>, TaskEventAdapter<TaskEvent, TaskEvent>>

    init {
      val adapterTypes: List<Class<TaskEventAdapter<*, *>>> =
        App.classScanner.getOrdinarySubclasses(TaskEventAdapter::class.java)
      adapterMap = buildAdapterMap(adapterTypes)
    }
  }

  private fun buildAdapterMap(adapterTypes: List<Class<out TaskEventAdapter<*, *>>>): ImmutableListMultimap<Class<*>, TaskEventAdapter<TaskEvent, TaskEvent>> {
    val builder = ImmutableListMultimap.builder<Class<*>, TaskEventAdapter<TaskEvent, TaskEvent>>()
    val pathBuilder = ImmutableListMultimap.builder<Class<*>, Class<*>>()
    @Suppress("UNCHECKED_CAST")
    for (adapterType in adapterTypes) {
      val typeArgs = Reflect.getTypeArgs(adapterType, TaskEventAdapter::class.java)
      val fromType: Class<out TaskEvent> = Reflect.getRawClass(typeArgs[0])
      val toType: Class<out TaskEvent> = Reflect.getRawClass(typeArgs[1])
      val adapter = Reflect.createInstance(adapterType) as TaskEventAdapter<TaskEvent, TaskEvent>
      builder.put(fromType, adapter)
      pathBuilder.put(fromType, toType)
    }
    checkCircularAdapter(pathBuilder.build())
    return builder.build()
  }

  private fun checkCircularAdapter(pathMap: ListMultimap<Class<*>, Class<*>>) {
    // check circular adapter
    fun visit(eventType: Class<*>, visited: MutableSet<Class<*>>, pathMap: ListMultimap<Class<*>, Class<*>>) {
      for (toType in pathMap.get(eventType)) {
        if (!visited.add(toType)) {
          throw IllegalStateException("circular adapter: ${visited.joinToString("->")}->$toType")
        } else {
          visit(toType, visited, pathMap)
        }
      }
    }

    for (key in pathMap.keys()) {
      val visited = LinkedHashSet<Class<*>>()
      visited.add(key)
      visit(key, visited, pathMap)
    }
  }


  fun adapt(event: TaskEvent): List<TaskEvent> {
    val adapters = AdapterMapHolder.adapterMap.get(event.javaClass)
    if (adapters.isEmpty()) {
      return emptyList()
    }
    return ImmutableList.copyOf(Lists.transform(adapters) { it.convert(event) })
  }
}
