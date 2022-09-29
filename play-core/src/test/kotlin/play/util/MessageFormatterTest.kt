package play.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

/**
 *
 *
 * @author LiangZengle
 */
internal class MessageFormatterTest {

    @Test
    fun format() {
      assertEquals("aaa", MessageFormatter.format("aaa"))
      assertEquals("{aaa", MessageFormatter.format("{aaa"))
      assertEquals("{a}aa", MessageFormatter.format("{a}aa"))
      assertEquals("a{}aa", MessageFormatter.format("a{}aa"))
      assertEquals("a1aa", MessageFormatter.format("a{0}aa", 1))
      assertEquals("a{1aa", MessageFormatter.format("a{{0}aa", 1))
      assertEquals("a1aa{", MessageFormatter.format("a{0}aa{", 1))
      assertEquals("aundefinedaa", MessageFormatter.format("a{1}aa", 1))
      assertEquals("a10aa", MessageFormatter.format("a{10}aa", 0, 1,2,3,4,5,6,7,8,9,10))
    }
}
