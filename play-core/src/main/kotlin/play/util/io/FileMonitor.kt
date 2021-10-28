package play.util.io

import play.util.logging.getLogger
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.math.max

open class FileMonitor internal constructor(
  val root: File,
  private val maxDepth: Int,
  protected val eventKinds: Array<WatchEvent.Kind<Path>>,
  private val createCallback: ((File) -> Unit)?,
  private val modifyCallback: ((File) -> Unit)?,
  private val deleteCallback: ((File) -> Unit)?
) : AutoCloseable {

  private val path = root.toPath()

  protected var service: WatchService = path.fileSystem.newWatchService()

  @Volatile
  private var closed = false

  protected open fun shouldReactTo(target: Path): Boolean = root.isDirectory || path == target

  @Suppress("UNCHECKED_CAST")
  protected fun process(key: WatchKey) {
    try {
      val events = key.pollEvents() as List<WatchEvent<Path>>
      if (events.isNotEmpty()) {
        handleEvents(path, events)
      }
    } finally {
      key.reset()
    }
  }

  protected open fun handleEvents(path: Path, events: List<WatchEvent<Path>>) {
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

  private fun watch(file: File, depth: Int) {
    val fileToWatch = if (file.isDirectory) {
      Files.walk(file.toPath(), depth).map { it.toFile() }.filter { it.isDirectory }
    } else {
      if (file.exists()) Stream.empty() else Stream.of(file.parentFile)
    }

    fileToWatch.forEach {
      try {
        it.toPath().register(service, eventKinds)
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
          if (closed) {
            break
          }
          val key = service.take()
          Thread.sleep(3000)
          process(key)
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

  override fun close() {
    closed = true
    service.close()
  }

  protected open fun onEvent(kind: WatchEvent.Kind<Path>, file: File) {
    when (kind) {
      StandardWatchEventKinds.ENTRY_CREATE -> createCallback?.invoke(file)
      StandardWatchEventKinds.ENTRY_MODIFY -> modifyCallback?.invoke(file)
      StandardWatchEventKinds.ENTRY_DELETE -> deleteCallback?.invoke(file)
      else -> logger.warn { "Unhandled event kind: $kind $file" }
    }
  }

  companion object {
    private val logger = getLogger()

    @JvmStatic
    fun builder() = Builder()

    @JvmStatic
    fun aggregatedBuilder() = AggregatedFileMonitor.Builder()
  }

  class Builder internal constructor() {
    private lateinit var file: File
    private var depth = Int.MAX_VALUE
    private var createCallback: ((File) -> Unit)? = null
    private var modifyCallback: ((File) -> Unit)? = null
    private var deleteCallback: ((File) -> Unit)? = null

    fun watchFileOrDir(file: File): Builder {
      this.file = file
      return this
    }

    fun depth(depth: Int): Builder {
      this.depth = depth
      return this
    }

    fun onCreate(action: (File) -> Unit): Builder {
      this.createCallback = action
      return this
    }

    fun onModify(action: (File) -> Unit): Builder {
      this.modifyCallback = action
      return this
    }

    fun onDelete(action: (File) -> Unit): Builder {
      this.deleteCallback = action
      return this
    }

    fun build(): FileMonitor {
      val list = ArrayList<WatchEvent.Kind<Path>>(3)
      if (createCallback != null) {
        list.add(StandardWatchEventKinds.ENTRY_CREATE)
      }
      if (modifyCallback != null) {
        list.add(StandardWatchEventKinds.ENTRY_MODIFY)
      }
      if (deleteCallback != null) {
        list.add(StandardWatchEventKinds.ENTRY_DELETE)
      }
      return FileMonitor(file, depth, list.toTypedArray(), createCallback, modifyCallback, deleteCallback)
    }
  }
}
