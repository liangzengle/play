package play.util.ranking

import org.eclipse.collections.api.map.primitive.IntObjectMap

/**
 *
 * @author LiangZengle
 */
interface IRankingList<ID : Comparable<ID>, E : RankingElement<ID>> {

  /**
   * 排行榜类型
   */
  val rankingType: RankingType<E>

  /**
   * Insert or update
   *
   * @param element
   */
  fun update(element: E)

  /**
   * Update if element exists
   *
   * @param element
   */
  fun updateIfExists(element: E)

  /**
   * 根据id获取其排名
   *
   * @param id
   * @return id对应的排名，如果不存在则返回0
   */
  fun getRankById(id: ID): Int

  /**
   * 获取部分排行榜数据
   *
   * @param fromRankInclusive 起始排名
   * @param toRankInclusive 截止排名
   * @param transformer 数据转换
   * @return List<T>
   */
  fun <T> subRank(fromRankInclusive: Int, toRankInclusive: Int, transformer: (Int, E) -> T): List<T>

  /**
   * 获取排行帮列表
   * @param transformer 数据转换
   * @return List<T>
   */
  fun <T> toList(transformer: (Int, E) -> T): List<T>

  /**
   * Convert to [IntObjectMap], which key is rank and value is element
   *
   * @return
   */
  fun toRankMap(): IntObjectMap<E>
}
