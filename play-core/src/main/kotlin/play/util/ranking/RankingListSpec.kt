package play.util.ranking

/**
 *  排行榜指标
 *
 * @author LiangZengle
 */
interface RankingListSpec<E> {

  /**
   * 元素类型
   * @return Class<E>
   */
  fun elementType(): Class<E>

  /**
   * 元素排序规则
   * @param o1 元素1
   * @param o2 元素2
   * @return -1:o1小于o2；0:o1等于o2； 1:o1大于o2
   */
  fun compare(o1: E, o2: E): Int

  /**
   * @return 排行榜容量
   */
  fun maxRankingSize(): Int

  /**
   * 排名门槛要求
   */
  fun rankRequirement(): RankRequirement<E>?

  /**
   * 排行榜数据更新
   * @param old 当前榜中的数据
   * @param change 变化的数据
   * @return 要更新的数据
   */
  fun update(old: E, change: E): E
}
