package play.util.io

import io.vavr.control.Option
import io.vavr.kotlin.none
import io.vavr.kotlin.some
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.time.Duration
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.math.max

open class FileMonitor(val root: File, val maxDepth: Int) : AutoCloseable {

  private val path = root.toPath()

  protected var service: WatchService = path.fileSystem.newWatchService()

  private var createCallback = Option.none<(File) -> Unit>()
  private var modifyCallback = Option.none<(File) -> Unit>()
  private var deleteCallback = Option.none<(File) -> Unit>()

  private var cancellable = none<ScheduledFuture<*>>()

  private var closed = AtomicBoolean()

  private val lastModifyTimeCache = ObjectLongHashMap<Path>()

  constructor(root: File, recursive: Boolean = true) : this(root, if (recursive) Int.MAX_VALUE else 0)

  protected fun interestedEvents() = arrayOf(
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_MODIFY,
    StandardWatchEventKinds.ENTRY_DELETE
  )

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
      if (file.exists()) Stream.empty<File>() else Stream.of(file.parentFile)
    }

    val interestedEvents = interestedEvents()
    fileToWatch.forEach {
      try {
        it.toPath().register(service, interestedEvents)
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
  }

  fun start() {
    watch(root, maxDepth)
    thread(start = true, isDaemon = true, name = "file-monitor[$path]") {
      while (true) {
        try {
          process(service.take())
        } catch (e: ClosedWatchServiceException) {
          if (!closed.get() && root.exists()) {
            service = path.fileSystem.newWatchService()
            watch(root, maxDepth)
          } else {
            throw e
          }
        }
      }
    }
  }

  fun start(detectInterval: Duration, executor: ScheduledExecutorService) {
    watch(root, maxDepth)
    val f = executor.scheduleAtFixedRate(
      {
        try {
          service.poll()?.also { process(it) }
        } catch (e: ClosedWatchServiceException) {
          if (!closed.get() && root.exists()) {
            service = path.fileSystem.newWatchService()
            watch(root, maxDepth)
          } else {
            throw e
          }
        }
      },
      detectInterval.toMillis(),
      detectInterval.toMillis(),
      TimeUnit.MILLISECONDS
    )
    cancellable = some(f)
  }

  override fun close() {
    closed.set(true)
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
    this.createCallback = some(Objects.requireNonNull(op))
    return this
  }

  fun onModify(op: (File) -> Unit): FileMonitor {
    this.modifyCallback = some(Objects.requireNonNull(op))
    return this
  }

  fun onDelete(op: (File) -> Unit): FileMonitor {
    this.deleteCallback = some(Objects.requireNonNull(op))
    return this
  }

  fun onCreateOrModify(op: (File) -> Unit): FileMonitor {
    onCreate(op)
    onModify(op)
    return this
  }
}
