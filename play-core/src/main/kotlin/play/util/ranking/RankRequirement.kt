package play.util.ranking

/**
 *
 * @author LiangZengle
 */
fun interface RankRequirement<T> {

  fun isSatisfied(rank: Int, element: T): Boolean
}
