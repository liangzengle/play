package play.db.mongo

import com.mongodb.client.model.Indexes
import org.bson.BsonDocument
import org.bson.conversions.Bson

/**
 *
 * @author LiangZengle
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class Index(
  val fields: Array<String>,
  val unique: Boolean,
  val sparse: Boolean = true,
  val type: Type = Type.AEC
) {

  enum class Type {
    AEC {
      override fun parse(fields: Array<String>): Bson {
        return Indexes.ascending(*fields)
      }
    },
    DEC {
      override fun parse(fields: Array<String>): Bson {
        return Indexes.descending(*fields)
      }
    },
    GEO_2D {
      override fun parse(fields: Array<String>): Bson {
        return Indexes.geo2d(fields[0])
      }
    },
    GEO_2D_SPHERE {
      override fun parse(fields: Array<String>): Bson {
        return Indexes.geo2dsphere(*fields)
      }
    },
    TEXT {
      override fun parse(fields: Array<String>): Bson {
        check(fields.size == 1) { "TEXT index do not support multiple fields." }
        return Indexes.text(fields[0])
      }
    },
    HASHED {
      override fun parse(fields: Array<String>): Bson {
        check(fields.size == 1) { "HASHED index do not support multiple fields." }
        return Indexes.hashed(fields[0])
      }
    },
    COMPOUND {
      override fun parse(fields: Array<String>): Bson {
        return Indexes.compoundIndex(fields.map(BsonDocument::parse))
      }
    };

    abstract fun parse(fields: Array<String>): Bson
  }
}
