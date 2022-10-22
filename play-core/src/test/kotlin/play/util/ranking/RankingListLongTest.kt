package play.util.ranking

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import play.util.json.Json
import play.util.unsafeCast

/**
 *
 * @author LiangZengle
 */
internal class RankingListLongTest {

  private val rankingList = RankingListLong<SimpleRankingElementLong>(TestRankingType.Score.unsafeCast())

  @BeforeEach
  fun setup() {
    rankingList.update(SimpleRankingElementLong(1, 95))
    rankingList.update(SimpleRankingElementLong(2, 93))
    rankingList.update(SimpleRankingElementLong(3, 70))
  }

  @Test
  fun insertOrUpdate() {
    assertEquals(2, rankingList.getRankById(1))
    assertEquals(3, rankingList.getRankById(2))
    assertEquals(4, rankingList.getRankById(3))
  }

  @Test
  fun update() {
    rankingList.updateIfExists(SimpleRankingElementLong(1, 100))
    assertEquals(1, rankingList.getRankById(1))

    rankingList.updateIfExists(SimpleRankingElementLong(4, 100))
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
  fun serialize() {
    val jsonString = Json.toJsonString(rankingList)
    println(jsonString)
    val list = Json.readValueAs<RankingListLong<SimpleRankingElementLong>>(jsonString)
    val map1 = list.toRankMap()
    val map0 = rankingList.toRankMap()
    assertEquals(map0.size(), map1.size())
    assertTrue(map1.keySet().containsAll(map0.keySet()))
    assertTrue(map1.values().containsAll(map0.values()))
  }
}
