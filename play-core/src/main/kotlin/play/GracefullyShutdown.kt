package play

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.util.Sorting
import play.util.concurrent.PlayFuture
import play.util.unsafeCast
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime

/**
 * 应用关闭时按顺序执行关闭任务
 *
 * Created by LiangZengle on 2020/2/16.
 */
interface GracefullyShutdown {

  /**
   * 添加一个关闭任务
   *
   * @param phase 关闭阶段的名称
   * @param name 任务的名称
   * @param ref 执行task的对象
   * @param task 要执行的任务
   */
  fun <T : Any> addTask(phase: String, name: String, ref: T, task: (T) -> PlayFuture<*>)

  fun run()

  companion object {

    const val PHASE_START = "shutdown-start"
    const val PHASE_SHUTDOWN_NETWORK_SERVICE = "shutdown-network-service"
    const val PHASE_SHUTDOWN_NETWORK_ACCEPTOR = "shutdown-network-acceptor"
    const val PHASE_SHUTDOWN_NETWORK_WORKER = "shutdown-network-worker"
    const val PHASE_SHUTDOWN_ACTOR_SYSTEM = "shutdown-actor-system"
    const val PHASE_SHUTDOWN_SCHEDULER = "shutdown-scheduler"
    const val PHASE_SHUTDOWN_APPLICATION_CONTEXT = "shutdown-application-context"
    const val PHASE_SHUTDOWN_DATABASE_SERVICE = "shutdown-database-service"
    const val PHASE_FLUSH_ENTITY_CACHE = "flush-entity-cache"

    @JvmStatic
    fun phaseFromConfig(config: Config): Phases {
      val defaultPhaseTimeout = config.getString("default-phase-timeout")
      val defaultPhaseConfig = ConfigFactory.parseString(
        """
          |{
          |  timeout = $defaultPhaseTimeout
          |  depends-on = []
          |}
        """.trimMargin()
      )
      val phasesConfig = config.getConfig("phases")
      val phaseMap = phasesConfig.root().keys.asSequence().map { k ->
        val c = phasesConfig.getConfig(k).withFallback(defaultPhaseConfig)
        val timeout = c.getDuration("timeout")
        val dependsOn = c.getStringList("depends-on")
        k to Phase(k, timeout, dependsOn.toSet())
      }.toMap()
      return Phases(phaseMap)
    }

    @JvmStatic
    fun topologicalSort(phases: Map<String, Phase>): List<String> {
      val result = LinkedList<String>()
      val unmarked = (phases.keys.asSequence() + phases.values.asSequence().flatMap { it.dependsOn }).toMutableSet()
      val tempMark = mutableSetOf<String>()

      fun depthFirstSearch(u: String) {
        if (tempMark.contains(u)) {
          throw IllegalArgumentException(
            "Cycle detected in graph of phases. It must be a DAG. phase [$u] depends transitively on itself. All dependencies: $phases"
          )
        }
        if (unmarked.contains(u)) {
          tempMark.add(u)
          phases[u]?.also {
            it.dependsOn.forEach(::depthFirstSearch)
          }
          unmarked.remove(u)
          tempMark.remove(u)
          result.addFirst(u)
        }
      }

      while (unmarked.isNotEmpty()) {
        depthFirstSearch(unmarked.first())
      }

      return result.reversed()
    }
  }

  data class Phase(val name: String, val timeout: Duration, val dependsOn: Set<String>)

  class Phases(phases: Map<String, Phase>) {
    private val sortedPhases = Sorting.topologicalSort(phases.values) { phase -> phase.dependsOn.map { phases[it]!! } }
    fun asList() = sortedPhases
  }
}

class DefaultGracefullyShutdown(
  private val applicationName: String,
  private val phases: GracefullyShutdown.Phases,
  runByJvmShutdownHook: Boolean,
  private val postShutdownAction: (() -> Unit)?
) : GracefullyShutdown {

  private val knownPhases = phases.asList().map { it.name }.toSet()

  private val phaseTasks = ConcurrentHashMap<String, List<Task<Any>>>()

  private var performed = false

  init {
    if (runByJvmShutdownHook) {
      Runtime.getRuntime().addShutdownHook(thread(false) { run() })
    }
  }

  @Synchronized
  override fun run() {
    if (performed) {
      Log.warn { "Shutdown should not be run more than once." }
      return
    }
    performed = true
    Log.info { "Application [$applicationName] shutting down..." }
    var anyFailure = false
    for (phase in phases.asList()) {
      val tasks = phaseTasks[phase.name] ?: continue
      if (tasks.isEmpty()) {
        continue
      }
      Log.debug { "Running Shutdown Phase [${phase.name}]" }
      for (task in tasks) {
        Log.debug { "Performing task [${task.name}] in phase [${phase.name}]" }
        try {
          val elapsed = measureNanoTime {
            val future = task.action(task.ref).timeout(phase.timeout)
            future.await()
          }
          Log.info { "Task [${task.name}] completed in ${TimeUnit.NANOSECONDS.toMillis(elapsed)}ms" }
        } catch (e: Exception) {
          anyFailure = true
          when (e) {
            is TimeoutException -> Log.error(e) { "Task [${task.name}] timeout" }
            else -> Log.error(e) { "Task [${task.name}] failed" }
          }
        }
      }
    }
    if (!anyFailure) {
      Log.info { "Application [$applicationName] shutdown successfully!" }
    } else {
      Log.error { "Application [$applicationName] shutdown EXCEPTIONALLY!!!" }
    }
    postShutdownAction?.invoke()
  }

  override fun <T : Any> addTask(phase: String, name: String, ref: T, task: (T) -> PlayFuture<*>) {
    require(knownPhases.contains(phase)) { "Unknown phase [$phase]" }
    require(name.isNotEmpty()) { "Task name must not be empty" }
    phaseTasks.merge(phase, listOf(Task(name, ref, task).unsafeCast())) { old, new -> old + new }
  }

  private class Task<T : Any>(val name: String, val ref: T, val action: (T) -> PlayFuture<*>)
}
