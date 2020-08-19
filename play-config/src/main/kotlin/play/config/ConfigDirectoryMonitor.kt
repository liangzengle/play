package play.config

import play.util.io.FileMonitor
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

/**
 * 配置文件变化监控：当[dir]目录中的文件发生变化时，对变化的文件执行[op]操作
 */
class ConfigDirectoryMonitor(dir: File, private val op: (Set<File>) -> Unit) : FileMonitor(dir) {
  override fun handlerEvents(path: Path, events: List<WatchEvent<Path>>) {
    super.handlerEvents(path, events)
    // 变化的文件批量操作
    val fileList = events.asSequence()
      .filter { it.kind() == StandardWatchEventKinds.ENTRY_CREATE || it.kind() == StandardWatchEventKinds.ENTRY_MODIFY }
      .map { path.resolve(it.context()).toFile() }
      .filter { it.isFile }
      .toSet()
    if (fileList.isNotEmpty()) {
      op(fileList)
    }
  }

  override fun onEvent(kind: WatchEvent.Kind<Path>, file: File) {
    // do nothing
  }
}
