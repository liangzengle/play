package play.util.io

import java.io.File
import java.nio.file.Path
import java.nio.file.WatchEvent

/**
 * 文件变化监听器
 *
 * @author LiangZengle
 */
fun interface FileChangeListener {

  /**
   * 处理文件变化
   *
   * @param kind 变化类型
   * @param file 变化的文件
   */
  fun onChange(kind: WatchEvent.Kind<Path>, file: File)

  /**
   * 文件变化处理完毕后调用
   *
   * @param root 监听的目标文件(夹)
   * @param events 所有的变化事件
   */
  fun afterChange(root: Path, events: List<WatchEvent<Path>>) {}
}

/**
 * 批量文件变化监听器
 *
 * @property action 处理逻辑
 */
class BatchFileChangeListener(private val action: (Set<File>) -> Unit) : FileChangeListener {
  private val fileSet = hashSetOf<File>()

  override fun onChange(kind: WatchEvent.Kind<Path>, file: File) {
    fileSet.add(file)
  }

  override fun afterChange(root: Path, events: List<WatchEvent<Path>>) {
    try {
      action(fileSet)
    } finally {
      fileSet.clear()
    }
  }
}
