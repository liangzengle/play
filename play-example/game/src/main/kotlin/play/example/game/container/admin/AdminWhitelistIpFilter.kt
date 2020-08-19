package play.example.game.container.admin

import play.net.http.BasicHttpRequest
import play.net.http.HttpRequestFilter

/**
 * 后台接口ip白名单过滤
 * @author LiangZengle
 */
class AdminWhitelistIpFilter : HttpRequestFilter() {
  override fun accept(request: BasicHttpRequest): Boolean {
    return AdminSettingConf.whiteList.contains(request.remoteHost())
  }
}
