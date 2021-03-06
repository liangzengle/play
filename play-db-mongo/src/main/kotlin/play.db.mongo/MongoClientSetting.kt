package play.db.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import de.undercouch.bson4jackson.BsonFactory
import de.undercouch.bson4jackson.BsonGenerator
import de.undercouch.bson4jackson.BsonParser
import org.bson.codecs.configuration.CodecRegistries
import play.Configuration
import play.db.EntityInt
import play.db.EntityLong
import play.db.EntityString
import play.db.mongo.codec.EntityCodecProvider
import play.db.mongo.codec.MongoIntIdMixIn
import play.db.mongo.codec.MongoLongIdMixIn
import play.db.mongo.codec.MongoStringIdMixIn
import play.util.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class MongoClientSettingBuilderProvider @Inject constructor(
  appConf: Configuration,
  @Named("mongodb") val conf: Configuration
) : Provider<MongoClientSettings.Builder> {

  private val threadPoolSize = appConf.getInt("db.thread-pool-size")

  override fun get(): MongoClientSettings.Builder {
    val db = conf.getString("db")
    val builder = MongoClientSettings.builder()
    builder.applicationName(db)
    if (conf.getBoolean("auth")) {
      val user = conf.getString("username")
      val pwd = conf.getString("password")
      builder.credential(MongoCredential.createCredential(user, db, pwd.toCharArray()))
    }
    val bsonFactory = BsonFactory()
      .enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH)
      .enable(BsonGenerator.Feature.WRITE_BIGDECIMALS_AS_DECIMAL128)
    val objectMapper = Json.configureDefault(ObjectMapper(bsonFactory))
      .addMixIn(EntityLong::class.java, MongoLongIdMixIn::class.java)
      .addMixIn(EntityInt::class.java, MongoIntIdMixIn::class.java)
      .addMixIn(EntityString::class.java, MongoStringIdMixIn::class.java)
    val entityCodecProvider = EntityCodecProvider(objectMapper)
    val codecRegistry = CodecRegistries.fromRegistries(
      CodecRegistries.fromProviders(entityCodecProvider),
      MongoClientSettings.getDefaultCodecRegistry()
    )
    builder.applyToConnectionPoolSettings {
      it.maxSize(threadPoolSize).maxWaitTime(10, TimeUnit.SECONDS)
    }
    builder.codecRegistry(codecRegistry)
    return builder
  }
}

@Singleton
class MongoClientSettingProvider @Inject constructor(private val builderProvider: MongoClientSettingBuilderProvider) :
  Provider<MongoClientSettings> {
  override fun get(): MongoClientSettings = builderProvider.get().build()
}
