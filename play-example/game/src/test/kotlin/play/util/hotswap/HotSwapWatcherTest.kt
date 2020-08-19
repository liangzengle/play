package play.util.hotswap

/**
 *
 * @author LiangZengle
 */
fun main() {
  System.setProperty("jdk.attach.allowAttachSelf", "true")
  HotSwapWatcher("C:\\Users\\liang\\Desktop\\test").start()

  Thread.sleep(10000000)
}
