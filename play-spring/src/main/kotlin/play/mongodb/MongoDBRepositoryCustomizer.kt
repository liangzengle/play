package play.mongodb

import play.db.mongo.MongoDBRepository
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
fun interface MongoDBRepositoryCustomizer {

  fun customize(repository: MongoDBRepository, classScanner: ClassScanner)
}
