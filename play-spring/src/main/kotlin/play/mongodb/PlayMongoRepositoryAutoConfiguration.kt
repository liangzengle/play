package play.mongodb

import com.mongodb.reactivestreams.client.MongoClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import play.db.DatabaseNameProvider
import play.db.TableNameResolver
import play.db.mongo.Mongo
import play.db.mongo.MongoDBRepository

/**
 *
 * @author LiangZengle
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Mongo::class)
@ConditionalOnProperty(prefix = "play.repository", name = ["type"], havingValue = "mongodb")
class PlayMongoRepositoryAutoConfiguration {

  @Bean
  fun repository(
    tableNameResolver: TableNameResolver,
    databaseNameProvider: DatabaseNameProvider,
    client: MongoClient,
    customizers: ObjectProvider<MongoDBRepositoryCustomizer>
  ): MongoDBRepository {
    val repository = MongoDBRepository(databaseNameProvider.get(), tableNameResolver, client)
    customizers.orderedStream().forEach { it.customize(repository) }
    return repository
  }
}
