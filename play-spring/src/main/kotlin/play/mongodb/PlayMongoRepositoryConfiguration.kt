package play.mongodb

import com.mongodb.reactivestreams.client.MongoClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.DatabaseNameProvider
import play.db.Repository
import play.db.TableNameResolver
import play.db.mongo.Mongo
import play.db.mongo.MongoDBRepository
import play.entity.Entity
import play.util.reflect.ClassScanner

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Mongo::class)
class PlayMongoRepositoryConfiguration {

  @Bean
  fun mongoDBRepositoryIndexCustomizer(): MongoDBRepositoryCustomizer {
    return MongoDBRepositoryCustomizer { repository, classScanner ->
      val entityClasses = classScanner.getInstantiatableSubclasses(Entity::class.java)
      Mongo.ensureIndexes(repository, entityClasses)
    }
  }

  @Bean
  @ConditionalOnMissingBean(Repository::class)
  fun repository(
    tableNameResolver: TableNameResolver,
    databaseNameProvider: DatabaseNameProvider,
    client: MongoClient,
    classScanner: ClassScanner,
    customizers: ObjectProvider<MongoDBRepositoryCustomizer>
  ): MongoDBRepository {
    val repository = MongoDBRepository(databaseNameProvider.get(), tableNameResolver, client)
    customizers.orderedStream().forEach { it.customize(repository, classScanner) }
    return repository
  }
}
