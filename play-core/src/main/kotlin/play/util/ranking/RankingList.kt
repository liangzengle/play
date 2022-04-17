package play.util.ranking

import org.eclipse.collections.api.map.primitive.IntObjectMap

/**
 *
 * @author LiangZengle
 */
abstract class RankingList<ID, E : RankingElement<ID>>(val rankingType: RankingType<E>) {

  /**
   * Insert or update
   *
   * @param element
   */
  abstract fun insertOrUpdate(element: E)

  /**
   * Update if element exists
   *
   * @param element
   */
  abstract fun update(element: E)

  /**
   * 根据id获取其排名
   *
   * @param id
   * @return id对应的排名，如果不存在则返回0
   */
  abstract fun getRankById(id: ID): Int

  abstract fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T>

  abstract fun <T> toList(transformer: (Int, E) -> T): List<T>

  /**
   * Convert to [IntObjectMap], which key is rank and value is element
   *
   * @return
   */
  abstract fun toRankMap(): IntObjectMap<E>
}
