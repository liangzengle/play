package play.mongodb

import play.db.mongo.MongoDBRepository

/**
 *
 * @author LiangZengle
 */
fun interface MongoDBRepositoryCustomizer {

  fun customize(repository: MongoDBRepository)
}
