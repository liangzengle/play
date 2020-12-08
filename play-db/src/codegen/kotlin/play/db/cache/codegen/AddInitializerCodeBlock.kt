package play.db.cache.codegen

import com.squareup.kotlinpoet.CodeBlock
import play.db.cache.*

/**
 *
 * @author LiangZengle
 */
object AddInitializerCodeBlock : EntityCacheComponent() {
  override fun apply() {
    cache.addInitializerBlock(
      CodeBlock.builder()
        .addStatement("val cacheSpec = entityClass.getAnnotation(%T::class.java)", CacheSpec)
        .beginControlFlow(
          "expireEvaluator = if (cacheSpec != null && cacheSpec.expireEvaluator != %T::class)",
          DefaultExpireEvaluator
        )
        .addStatement("injector.getInstance(cacheSpec.expireEvaluator.java)")
        .nextControlFlow("else")
        .addStatement("injector.getInstance(%T::class.java)", DefaultExpireEvaluator)
        .endControlFlow()
        .beginControlFlow("if (cacheSpec?.loadAllOnInit == true)")
        .addStatement("""%T.info { "loading all [${'$'}{entityClass.simpleName}]" }""", Log)
        .beginControlFlow("queryService.foreach(entityClass)")
        .addStatement("it.postLoad()")
        .addStatement("cache[it.%L] = CacheObj(it, %M())", getId(), currentMillis)
        .endControlFlow()
        .addStatement(".await(1.%M)", minutes)
        .addStatement("""Log.info { "loaded ${'$'}{cache.size} [${'$'}{entityClass.simpleName}]" }""")
        .endControlFlow()
        //
        .addStatement("val persistStrategy = cacheSpec?.persistStrategy ?: CacheSpec.PersistStrategy.Scheduled")
        .beginControlFlow("if (persistStrategy == CacheSpec.PersistStrategy.Scheduled)")
        .addStatement("scheduler.scheduleAtFixedRate(conf.persistInterval, conf.persistInterval.dividedBy(2), executor, createPersistTask())")
        .nextControlFlow("else")
        .addStatement(
          """Log.info { "[${'$'}{entityClass.simpleName}] using [${'$'}persistStrategy] persist strategy." }"""
        )
        .endControlFlow()
        //
        .beginControlFlow("if (expireEvaluator !is %T)", NeverExpireEvaluator)
        .addStatement("scheduler.scheduleAtFixedRate(conf.expireAfterAccess, conf.expireAfterAccess.dividedBy(2),  executor, createExpirationTask())")
        .nextControlFlow("else")
        .addStatement("""Log.info { "[${'$'}{entityClass.simpleName}] will never expire." }""")
        .endControlFlow()
        .build()
    )
  }

  override fun accept(): Boolean {
    return true
  }
}
