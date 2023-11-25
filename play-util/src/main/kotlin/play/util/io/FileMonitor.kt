package play.util.io

import play.util.logging.WithLogger
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.stream.Stream
import kotlin.concurrent.thread
import kotlin.math.max

/**
 * 文件变化监听器
 *
 * @property root 监听的目标文件(夹)
 * @property maxDepth 监听文件层级，不限使用[Int.MAX_VALUE]
 * @property eventKinds 监听的变化类型
 * @property listener 变化处理器
 * @constructor
 */
class FileMonitor internal constructor(
  val root: File,
  private val maxDepth: Int,
  private val eventKinds: Array<WatchEvent.Kind<Path>>,
  private val listener: FileChangeListener
) : AutoCloseable {

  companion object : WithLogger() {
    /**
     * 文件创建和修改
     */
    @JvmStatic
    val CREATE_AND_MODIFY = arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)

    /**
     * 启动一个文件监听器
     *
     * @param target 监听的目标文件(夹)
     * @param depth 监听文件层级
     * @param eventKinds 监听的变化类型
     * @param listener 变化处理器
     */
    @JvmStatic
    fun start(
      target: File,
      depth: Int = Int.MAX_VALUE,
      eventKinds: Array<WatchEvent.Kind<Path>> = CREATE_AND_MODIFY,
      listener: FileChangeListener
    ): FileMonitor {
      val monitor = FileMonitor(target, depth, eventKinds, listener)
      monitor.start()
      return monitor
    }

    /**
     * 启动一个文件监听器
     *
     * @param target 监听的目标文件(夹)
     * @param action 变化处理器
     */
    @JvmStatic
    fun start(target: File, action: (WatchEvent.Kind<Path>, File) -> Unit): FileMonitor {
      return start(target = target, listener = { kind, file -> action(kind, file) })
    }

    /**
     * 启动一个文件监听器
     *
     * @param target 监听的目标文件(夹)
     * @param action 批量文件变化处理器
     */
    @JvmStatic
    fun start(target: File, action: (Set<File>) -> Unit): FileMonitor {
      return start(target = target, listener = BatchFileChangeListener(action))
    }
  }

  private val path = root.toPath()

  private var service: WatchService = path.fileSystem.newWatchService()

  @Volatile
  private var closed = false

  private fun shouldReactTo(target: Path): Boolean = root.isDirectory || path == target

  @Suppress("UNCHECKED_CAST")
  private fun process(key: WatchKey) {
    try {
      val events = key.pollEvents() as List<WatchEvent<Path>>
      if (events.isNotEmpty()) {
        handleEvents(path, events)
        listener.afterChange(path, events)
      }
    } catch (e: Exception) {
      logger.error(e) { "Exception occurred when processing file events at ${root.absolutePath}" }
    } finally {
      key.reset()
    }
  }

  private fun handleEvents(path: Path, events: List<WatchEvent<Path>>) {
    for (event in events) {
      val target = path.resolve(event.context())
      if (shouldReactTo(target)) {
        val file = target.toFile()
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          val depth = path.relativize(target).nameCount
          watch(file, max((maxDepth - depth), 0))
        }
        if (file.isFile) {
          listener.onChange(event.kind(), file)
        }
      }
    }
  }

  private fun watch(file: File, depth: Int) {
    val fileToWatch = if (file.isDirectory) {
      Files.walk(file.toPath(), depth).map { it.toFile() }.filter { it.isDirectory }
    } else {
      if (!file.exists()) Stream.empty() else Stream.of(file.parentFile)
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
          Thread.sleep(5000)
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
}
