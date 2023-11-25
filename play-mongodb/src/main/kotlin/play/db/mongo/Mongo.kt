@file:JvmName("Mongo")

package play.db.mongo

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.connection.netty.NettyStreamFactoryFactory
import com.typesafe.config.Config
import de.undercouch.bson4jackson.BsonFactory
import de.undercouch.bson4jackson.BsonGenerator
import de.undercouch.bson4jackson.BsonParser
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

import org.bson.codecs.configuration.CodecRegistries
import play.db.mongo.codec.EntityCodecProvider
import play.db.mongo.codec.MongoIntIdMixIn
import play.db.mongo.codec.MongoLongIdMixIn
import play.db.mongo.codec.MongoObjIdMixIn
import play.entity.*
import play.entity.cache.CacheIndex
import play.util.LambdaClassValue
import play.util.isAssignableFrom
import play.util.json.Json
import play.util.mkString
import play.util.reflect.Reflect
import reactor.core.publisher.Flux
import java.util.concurrent.TimeUnit

object Mongo : WithLogger() {

  const val ID = "_id"

  private val cacheIndexNameCache = LambdaClassValue { type ->
    getCacheIndexName(type) ?: throw IllegalArgumentException("CacheIndex not found in class: ${type.name}")
  }

  fun newClientSettings(conf: Config): MongoClientSettings {
    return newClientSettingsBuilder(conf).build()
  }

  fun newClientSettingsBuilder(conf: Config): MongoClientSettings.Builder {
    val builder = MongoClientSettings.builder()
    if (conf.getBoolean("auth")) {
      val user = conf.getString("username")
      val pwd = conf.getString("password")
      val authSource = conf.getString("auth-source")
      builder.credential(MongoCredential.createCredential(user, authSource, pwd.toCharArray()))
    }
    val bsonFactory = BsonFactory().enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH)
      .enable(BsonGenerator.Feature.WRITE_BIGDECIMALS_AS_DECIMAL128)
    val objectMapper = Json.configure(ObjectMapper(bsonFactory)).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
      .addMixIn(LongIdEntity::class.java, MongoLongIdMixIn::class.java)
      .addMixIn(IntIdEntity::class.java, MongoIntIdMixIn::class.java)
      .addMixIn(ObjIdEntity::class.java, MongoObjIdMixIn::class.java)
    val entityCodecProvider = EntityCodecProvider(objectMapper)
    val codecRegistry = CodecRegistries.fromRegistries(
      CodecRegistries.fromProviders(entityCodecProvider), MongoClientSettings.getDefaultCodecRegistry()
    )
    val nThread = conf.getInt("client-threads")
    builder.applyToConnectionPoolSettings {
      it.maxSize(nThread).maxWaitTime(10, TimeUnit.SECONDS)
    }
    builder.codecRegistry(codecRegistry)
    return builder
  }

  fun newNettyStreamFactory(eventLoopGroup: EventLoopGroup): NettyStreamFactoryFactory {
    val builder = NettyStreamFactoryFactory.builder()
    if (eventLoopGroup is EpollEventLoopGroup) {
      builder.eventLoopGroup(eventLoopGroup).socketChannelClass(EpollSocketChannel::class.java)
    } else {
      builder.eventLoopGroup(eventLoopGroup).socketChannelClass(NioSocketChannel::class.java)
    }
    return builder.build()
  }

  fun ensureIndexes(repository: MongoDBRepository, entityClasses: Collection<Class<Entity<*>>>) {
    Flux.fromIterable(entityClasses).flatMap { entityClass ->
      val indexModels = getIndexModels(entityClass)
      if (indexModels.isEmpty()) {
        Flux.empty()
      } else {
        Flux.from(repository.getCollection(entityClass).createIndexes(indexModels))
          .flatMap { Flux.from(repository.getCollection(entityClass).listIndexes()).collectList() }
          .doOnNext {
            logger.debug { "Indexes created for ${entityClass.simpleName}: $it" }
          }.doOnError {
            logger.error(it) { "Index create failed: ${entityClass.simpleName}" }
          }
      }
    }.ignoreElements().block()
  }

  fun getCacheIndexName(entityClass: Class<out Entity<*>>): String {
    return cacheIndexNameCache.get(entityClass)
  }

  private fun getIndexModels(entityClass: Class<Entity<*>>): List<IndexModel> {
    val indexSequence = entityClass.getAnnotationsByType(Index::class.java).asSequence().map(::toIndexModel)
    val cacheIndex = getCacheIndex(entityClass)
    return if (cacheIndex != null) {
      (indexSequence + cacheIndex).toList()
    } else {
      indexSequence.toList()
    }
  }

  private fun getCacheIndex(entityClass: Class<Entity<*>>): IndexModel? {
    val indexModel = getCacheIndex0(entityClass)
    if (indexModel != null) {
      return indexModel
    }
    val idClass = EntityHelper.getIdClass(entityClass)
    if (isAssignableFrom<ObjId>(idClass)) {
      return getCacheIndex0(idClass, listOf(ID))
    }
    return null
  }

  private fun getCacheIndex0(clazz: Class<*>, parents: List<String> = emptyList()): IndexModel? {
    val indexName = getCacheIndexName(clazz, parents) ?: return null
    val fields = arrayOf(indexName)
    val opts = IndexOptions().unique(false).sparse(true)
    val indexBson = Index.Type.AEC.parse(fields)
    return IndexModel(indexBson, opts)
  }

  private fun getCacheIndexName(clazz: Class<*>, parents: List<String> = emptyList()): String? {
    return Reflect.getAllFields(clazz)
      .firstOrNull { it.isAnnotationPresent(CacheIndex::class.java) }
      ?.let { field ->
        val actualFieldName = field.getAnnotation(CacheIndex::class.java).fieldName
        val indexFieldName = if (parents.isEmpty()) {
          actualFieldName.ifEmpty { field.name }
        } else {
          parents.mkString('.', "", '.') + actualFieldName.ifEmpty { field.name }
        }
        indexFieldName
      }
  }

  private fun toIndexModel(index: Index): IndexModel {
    if (index.fields.isEmpty()) {
      throw IllegalArgumentException("Index must have at least one field")
    }
    val fields = index.fields.copyOf()
    for (i in fields.indices) {
      val field = fields[i]
      if (field.startsWith("id.")) {
        fields[i] = "_$field"
      }
    }
    val opts = IndexOptions()
    opts.unique(index.unique)
    opts.sparse(index.sparse)
    val indexBson = index.type.parse(fields)
    return IndexModel(indexBson, opts)
  }
}
