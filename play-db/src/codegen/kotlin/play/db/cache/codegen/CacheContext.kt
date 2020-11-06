package play.db.cache.codegen

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

/**
 *
 * @author LiangZengle
 */
class CacheContext(
  val className: String,
  val idType: TypeName
) {
  lateinit var cache: TypeSpec.Builder
}
