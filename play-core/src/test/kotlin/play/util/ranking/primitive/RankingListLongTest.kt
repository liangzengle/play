package play.util.ranking.primitive

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import play.util.ranking.TestRankingType
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
internal class RankingListLongTest {

  private val rankingList = RankingListLong<SimpleRankingElementLong>(TestRankingType.Score.unsafeCast())

  @BeforeEach
  fun setup() {
    rankingList.insertOrUpdate(SimpleRankingElementLong(1, 95))
    rankingList.insertOrUpdate(SimpleRankingElementLong(2, 93))
    rankingList.insertOrUpdate(SimpleRankingElementLong(3, 70))
  }

  @Test
  fun insertOrUpdate() {
    assertEquals(2, rankingList.getRankById(1))
    assertEquals(3, rankingList.getRankById(2))
    assertEquals(4, rankingList.getRankById(3))
  }

  @Test
  fun update() {
    rankingList.update(SimpleRankingElementLong(1, 100))
    assertEquals(1, rankingList.getRankById(1))
  }

  @Test
  fun subRank() {
    val map = rankingList.subRank(1, 2) { rank, element -> rank to element.id }.toMap()
    assertEquals(1, map.size)
    assertNull(map[1])
    assertNotNull(map[2])
  }

  @Test
  fun toGeneric() {
    val list1 = rankingList.toList { rank, element -> rank to element.id }.toList()
    val list2 = rankingList.toGeneric().toList { rank, element -> rank to element.id() }.toList()
    assertTrue(list1 == list2)
  }
}
