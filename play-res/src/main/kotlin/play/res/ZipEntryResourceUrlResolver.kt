package play.res

import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * 所有的配置打包成了zip文件
 *
 * @author LiangZengle
 */
class ZipEntryResourceUrlResolver(zipFilePath: Path) : ResourceUrlResolver(zipFilePath.toUri()) {

  private val fileSystem = FileSystems.newFileSystem(zipFilePath, null as ClassLoader?)

  override fun resolve(relativePath: String): Result<URL> {
    return runCatching { fileSystem.getPath("/$relativePath").toUri().toURL() }
  }
}
