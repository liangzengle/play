package play.entity.cache.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import play.entity.cache.ID_Any
import java.io.File

/**
 *
 * @author LiangZengle
 */
object EntityCacheGenerator {
  private val components = listOf(
    InitCache,
    AddConstructor,
    AddProperties,
    AddInitializerCodeBlock,
    ImplementFunctions,
    AddPrivateFunctions,
    AddCacheObj
  )

  private val contexts = listOf(
    CacheContext("EntityCacheIntImpl", INT),
    CacheContext("EntityCacheLongImpl", LONG),
    CacheContext("EntityCacheImpl", ID_Any),
  )

  @JvmStatic
  fun main(args: Array<String>) {
    val dir = args[0].replace('.', '/')
    println(dir)
    contexts.forEach { ctx ->
      components.forEach {
        it.apply(ctx)
      }
      FileSpec.builder("play.entity.cache.chm", ctx.className)
        .addType(ctx.cache.build())
        .build()
        .writeTo(File(dir))
    }
  }
}
