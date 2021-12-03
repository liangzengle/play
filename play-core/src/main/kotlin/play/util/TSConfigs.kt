package play.util

import com.google.common.collect.Sets
import com.typesafe.config.*
import com.typesafe.config.impl.ConfigImplUtil
import java.net.URL

/**
 * typesafe config
 *
 * @author LiangZengle
 */
object TSConfigs {

  fun Config.toJson() = root().render(ConfigRenderOptions.concise())

  /**
   * 递归地获取通过`include`关联的所有配置文件(包含[configUrl])
   *
   * @param configUrl 配置文件的url
   * @param includeSelf 是否包含自身(即[configUrl])
   * @return 所有通过include包含的配置文件的url, 包含自身
   */
  fun getIncludedUrls(configUrl: URL, includeSelf: Boolean): Set<URL> {
    val syntax = ConfigImplUtil.syntaxFromExtension(configUrl.path)
    if (syntax != null && syntax != ConfigSyntax.CONF) {
      return if (includeSelf) setOf(configUrl) else emptySet()
    }
    val config = ConfigFactory.parseURL(configUrl)
    val configObject = config.root()
    val result = Sets.newHashSetWithExpectedSize<URL>(2)
    findIncludedUrls(configObject, result, configUrl)
    if (result.isEmpty()) {
      return if (includeSelf) setOf(configUrl) else emptySet()
    }
    if (includeSelf) {
      result.add(configUrl)
    }
    return result
  }

  private fun findIncludedUrls(configObject: ConfigObject, urls: MutableSet<URL>, excludedUrl: URL) {
    for ((_, v) in configObject.entries) {
      val url = v.origin().url()
      if (v is ConfigObject) {
        findIncludedUrls(v, urls, excludedUrl)
      } else {
        if (url != null && url != excludedUrl) {
          urls.add(url)
        }
      }
    }
  }
}
