package play.util.io

import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap
import play.getLogger
import play.util.forEach
import play.util.scheduling.Scheduler
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.time.Duration
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.math.max

open class FileMonitor(val root: File, val maxDepth: Int) : AutoCloseable {
  private val logger = getLogger()

  private val path = root.toPath()

  protected var service: WatchService = path.fileSystem.newWatchService()

  private var createCallback = Optional.empty<(File) -> Unit>()
  private var modifyCallback = Optional.empty<(File) -> Unit>()
  private var deleteCallback = Optional.empty<(File) -> Unit>()

  private var cancellable = Optional.empty<ScheduledFuture<*>>()

  @Volatile
  private var closed = false

  private val lastModifyTimeCache = ObjectLongHashMap<Path>()

  constructor(root: File, recursive: Boolean = true) : this(root, if (recursive) Int.MAX_VALUE else 0)

  protected fun interestedEvents() = DefaultInterestedEvents

  protected open fun shouldReactTo(target: Path): Boolean = root.isDirectory || path == target

  @Suppress("UNCHECKED_CAST")
  protected fun process(key: WatchKey) {
    try {
      val events = key.pollEvents() as List<WatchEvent<Path>>
      if (events.isNotEmpty()) {
        val path = key.watchable() as Path
        val modifiedList = mutableListOf<WatchEvent<Path>>()
        for (event in events) {
          val lastModified = path.resolve(event.context()).toFile().lastModified()
          val lastModifiedTime = lastModifyTimeCache.get(event.context())
          if (lastModifiedTime != lastModified || event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            lastModifyTimeCache.put(event.context(), lastModified)
            modifiedList.add(event)
          }
        }
        handlerEvents(path, modifiedList)
      }
    } finally {
      key.reset()
    }
  }

  protected open fun handlerEvents(path: Path, events: List<WatchEvent<Path>>) {
    for (event in events) {
      val target = path.resolve(event.context())
      if (shouldReactTo(target)) {
        val file = target.toFile()
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          val depth = path.relativize(target).nameCount
          watch(file, max((maxDepth - depth), 0))
        }
        if (file.isFile) {
          onEvent(event.kind(), file)
        }
      }
    }
  }

  protected fun watch(file: File, depth: Int) {
    val fileToWatch = if (file.isDirectory) {
      Files.walk(file.toPath(), depth).map { it.toFile() }.filter { it.isDirectory && it.exists() }
    } else {
      if (file.exists()) Stream.empty() else Stream.of(file.parentFile)
    }

    val interestedEvents = interestedEvents()
    fileToWatch.forEach {
      try {
        it.toPath().register(service, interestedEvents)
      } catch (e: IOException) {
        logger.error(e) { "Failed to register to FileService: $it" }
      }
    }
  }

  fun start() {
    watch(root, maxDepth)
    thread(start = true, isDaemon = true, name = "file-monitor[$path]") {
      while (!Thread.currentThread().isInterrupted) {
        try {
          process(service.take())
        } catch (e: ClosedWatchServiceException) {
          if (!closed && root.exists()) {
            service = path.fileSystem.newWatchService()
            watch(root, maxDepth)
          } else {
            throw e
          }
        }
      }
    }
  }

  fun start(detectInterval: Duration, scheduler: Scheduler) {
    watch(root, maxDepth)
    val f = scheduler.scheduleWithFixedDelay(detectInterval, detectInterval, this::runCheck)
    cancellable = Optional.of(f)
  }

  fun start(detectInterval: Duration, scheduler: ScheduledExecutorService, executor: Executor) {
    watch(root, maxDepth)
    val f = scheduler.scheduleWithFixedDelay(
      { executor.execute(this::runCheck) },
      detectInterval.toMillis(),
      detectInterval.toMillis(),
      TimeUnit.MILLISECONDS
    )
    cancellable = Optional.of(f)
  }

  private fun runCheck() {
    try {
      service.poll()?.also { process(it) }
    } catch (e: ClosedWatchServiceException) {
      if (!closed && root.exists()) {
        service = path.fileSystem.newWatchService()
        watch(root, maxDepth)
      } else {
        logger.error(e) { e.message }
      }
    }
  }

  override fun close() {
    closed = true
    service.close()
    cancellable.forEach { it.cancel(false) }
  }

  protected open fun onEvent(kind: WatchEvent.Kind<Path>, file: File) {
    when (kind) {
      StandardWatchEventKinds.ENTRY_CREATE -> createCallback.forEach { it(file) }
      StandardWatchEventKinds.ENTRY_MODIFY -> modifyCallback.forEach { it(file) }
      StandardWatchEventKinds.ENTRY_DELETE -> deleteCallback.forEach { it(file) }
    }
  }

  fun onCreate(op: (File) -> Unit): FileMonitor {
    this.createCallback = Optional.of(Objects.requireNonNull(op))
    return this
  }

  fun onModify(op: (File) -> Unit): FileMonitor {
    this.modifyCallback = Optional.of(Objects.requireNonNull(op))
    return this
  }

  fun onDelete(op: (File) -> Unit): FileMonitor {
    this.deleteCallback = Optional.of(Objects.requireNonNull(op))
    return this
  }

  fun onCreateOrModify(op: (File) -> Unit): FileMonitor {
    onCreate(op)
    onModify(op)
    return this
  }

  companion object {
    val DefaultInterestedEvents = arrayOf(
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_MODIFY,
      StandardWatchEventKinds.ENTRY_DELETE
    )
  }
}
