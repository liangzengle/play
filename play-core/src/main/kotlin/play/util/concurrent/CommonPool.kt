package play.util.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

/**
 * @author LiangZengle
 */
object CommonPool : ExecutorService by ForkJoinPool.commonPool()
