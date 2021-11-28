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
object TSConfig {

  fun Config.toJson() = root().render(ConfigRenderOptions.concise())

  /**
   * 递归地获取通过`include`关联的所有配置文件(包含[configUrl])
   *
   * @param configUrl 配置文件的url
   * @return 所有通过include包含的配置文件的url, 包含自身
   */
  fun getIncludedUrls(configUrl: URL): Set<URL> {
    val syntax = ConfigImplUtil.syntaxFromExtension(configUrl.path)
    if (syntax != null && syntax != ConfigSyntax.CONF) {
      return setOf(configUrl)
    }
    val config = ConfigFactory.parseURL(configUrl)
    val configObject = config.root()
    val result = Sets.newHashSetWithExpectedSize<URL>(2)
    findIncludedUrls(configObject, result)
    return result
  }

  private fun findIncludedUrls(configObject: ConfigObject, urls: MutableSet<URL>) {
    for ((_, v) in configObject.entries) {
      val url = v.origin().url()
      if (v is ConfigObject) {
        if (url != null) {
          findIncludedUrls(v, urls)
        }
      } else {
        urls.add(url)
      }
    }
  }
}
