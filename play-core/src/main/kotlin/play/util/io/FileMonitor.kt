package play.util.io

import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap
import play.util.logging.getLogger
import play.scheduling.Cancellable
import play.scheduling.Scheduler
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.time.Duration
import java.util.concurrent.Executor
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.math.max

open class FileMonitor(val root: File, val maxDepth: Int) : AutoCloseable {

  private val path = root.toPath()

  protected var service: WatchService = path.fileSystem.newWatchService()

  private var createCallback: ((File) -> Unit)? = null
  private var modifyCallback: ((File) -> Unit)? = null
  private var deleteCallback: ((File) -> Unit)? = null

  private var cancellable:Cancellable? = null

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
      Files.walk(file.toPath(), depth).map { it.toFile() }.filter { it.isDirectory }
    } else {
      if (file.exists()) Stream.empty() else Stream.of(file.parentFile)
    }

    val interestedEvents = interestedEvents()
    fileToWatch.forEach {
      try {
        it.toPath().register(service, interestedEvents)
      } catch (e: IOException) {
        logger.error(e) { "Failed to watch: $it" }
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
    cancellable = f
  }

  fun start(detectInterval: Duration, scheduler: Scheduler, executor: Executor) {
    watch(root, maxDepth)
    val f = scheduler.scheduleWithFixedDelay(
      detectInterval,
      detectInterval,
      executor,
      this::runCheck
    )
    cancellable = f
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
    cancellable?.cancel()
  }

  protected open fun onEvent(kind: WatchEvent.Kind<Path>, file: File) {
    when (kind) {
      StandardWatchEventKinds.ENTRY_CREATE -> createCallback?.invoke(file)
      StandardWatchEventKinds.ENTRY_MODIFY -> modifyCallback?.invoke(file)
      StandardWatchEventKinds.ENTRY_DELETE -> deleteCallback?.invoke(file)
    }
  }

  fun onCreate(op: (File) -> Unit): FileMonitor {
    this.createCallback = op
    return this
  }

  fun onModify(op: (File) -> Unit): FileMonitor {
    this.modifyCallback = op
    return this
  }

  fun onDelete(op: (File) -> Unit): FileMonitor {
    this.deleteCallback = op
    return this
  }

  fun onCreateOrModify(op: (File) -> Unit): FileMonitor {
    onCreate(op)
    onModify(op)
    return this
  }

  companion object {
    private val logger = getLogger()

    private val DefaultInterestedEvents = arrayOf(
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_MODIFY,
      StandardWatchEventKinds.ENTRY_DELETE
    )
  }
}
