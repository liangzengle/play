package play.util.io

import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

/**
 *
 * @author LiangZengle
 */
class AggregatedFileMonitor private constructor(
  root: File,
  depth: Int,
  eventKinds: Array<WatchEvent.Kind<Path>>,
  private val action: (Set<File>) -> Unit
) : FileMonitor(root, depth, eventKinds, null, null, null) {

  override fun shouldReactTo(target: Path): Boolean {
    return super.shouldReactTo(target)
  }

  override fun handleEvents(path: Path, events: List<WatchEvent<Path>>) {
    super.handleEvents(path, events)
    // 变化的文件批量操作
    val fileList = events.asSequence()
      .filter { eventKinds.contains(it.kind()) }
      .map { path.resolve(it.context()).toFile() }
      .filter { it.isFile }
      .toSet()
    if (fileList.isNotEmpty()) {
      action(fileList)
    }
  }

  override fun onEvent(kind: WatchEvent.Kind<Path>, file: File) {
  }

  class Builder internal constructor() {
    private lateinit var file: File
    private var depth = Int.MAX_VALUE
    private val eventKinds = ArrayList<WatchEvent.Kind<Path>>(3)
    private lateinit var action: (Set<File>) -> Unit

    fun watchFileOrDir(file: File): Builder {
      this.file = file
      return this
    }

    fun depth(depth: Int): Builder {
      this.depth = depth
      return this
    }

    fun watchCreate(): Builder {
      watch(StandardWatchEventKinds.ENTRY_CREATE)
      return this
    }

    fun watchModify(): Builder {
      watch(StandardWatchEventKinds.ENTRY_MODIFY)
      return this
    }

    fun watchDelete(): Builder {
      watch(StandardWatchEventKinds.ENTRY_DELETE)
      return this
    }

    fun watch(eventKind: WatchEvent.Kind<Path>): Builder {
      if (!eventKinds.contains(eventKind)) {
        eventKinds.add(eventKind)
      }
      return this
    }

    fun onFileChange(action: (Set<File>) -> Unit): Builder {
      this.action = action
      return this
    }

    fun build(): FileMonitor {
      check(eventKinds.isNotEmpty()) { "no event watched." }
      return AggregatedFileMonitor(file, depth, eventKinds.toTypedArray(), action)
    }
  }
}
