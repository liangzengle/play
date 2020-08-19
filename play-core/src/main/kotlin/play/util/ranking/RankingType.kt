package play.util.ranking

/**
 *
 * @author LiangZengle
 */
interface RankingType<E> {

  fun specification(): RankingListSpecification<E>
}
