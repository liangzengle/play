package play.util.ranking

/**
 *
 * @author LiangZengle
 */
interface RankingListSpecification<E> {

  fun elementType(): Class<E>

  fun comparator(): Comparator<E>

  fun maxRankingSize(): Int

  fun rankRequirement(): RankRequirement<E>?
}
