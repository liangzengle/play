package play.util.ranking

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 *  排行榜类型
 * @author LiangZengle
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
interface RankingType<E> {

  fun spec(): RankingListSpec<E>
}
