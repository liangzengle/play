package play.util.hotswap

import play.hotswap.HotSwapWatcher

/**
 *
 * @author LiangZengle
 */
fun main() {
  System.setProperty("jdk.attach.allowAttachSelf", "true")
  HotSwapWatcher("C:\\Users\\liang\\Desktop\\test").start()

  Thread.sleep(10000000)
}
