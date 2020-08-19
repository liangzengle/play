package play.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 *
 * @author LiangZengle
 */
internal class TSConfigsTest {

  @Test
  fun findIncludedConfigUrls() {
    val aUrl = Thread.currentThread().contextClassLoader.getResource("a.conf")!!
    val bUrl = Thread.currentThread().contextClassLoader.getResource("b.conf")!!
    val cUrl = Thread.currentThread().contextClassLoader.getResource("c.conf")!!
    val urls = TSConfigs.getIncludedUrls(aUrl, true)
    val expected = setOf(aUrl, bUrl, cUrl)
    assertEquals(urls.size, expected.size)
    assert(urls == expected)
  }
}
