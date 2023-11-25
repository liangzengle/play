package play.ranking

/**
 * 排名要求，如第1名要求积分达到100
 *
 * @author LiangZengle
 */
fun interface RankRequirement<E> {

  /**
   * 判断[element]是否满足排名[rank]要求的门槛
   * @param rank 排名
   * @param element 排行的元素
   * @return 是否满足
   */
  fun isSatisfied(rank: Int, element: E): Boolean
}
