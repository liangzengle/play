package play.mongodb

import com.mongodb.MongoClientSettings

/**
 *
 * @author LiangZengle
 */
interface MongoClientSettingsCustomizer {
  fun customize(builder: MongoClientSettings.Builder)
}
