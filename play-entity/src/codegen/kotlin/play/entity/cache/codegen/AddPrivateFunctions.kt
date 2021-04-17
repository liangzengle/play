package play.entity.cache.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import play.entity.cache.E_Entity_ID
import play.entity.cache.Function_Unit
import play.entity.cache.getCause

/**
 *
 * @author LiangZengle
 */
object AddPrivateFunctions : EntityCacheComponent() {
  override fun accept(): Boolean {
    return true
  }

  override fun apply() {
    createPersistTask()
    createExpirationTask()
    writeToDB()
    delete()
    isDeleted()
    requireNotDeleted()
    computeIfAbsent1()
    computeIfAbsent2()
    entitySequence()
  }

  private fun createPersistTask() {
    cache.addFunction(
      FunSpec.builder("createPersistTask")
        .addModifiers(KModifier.PRIVATE)
        .returns(Function_Unit)
        .beginControlFlow("return")
        .addStatement(
          """
        val now = currentMillis()
        val persistTimeThreshold = now - conf.persistInterval.toMillis()
        val entities = cache.values.asSequence()
          .filter { it.hasEntity() && it.lastPersistTime < persistTimeThreshold }
          .map {
            it.lastPersistTime = now
            it.getEntitySilently()!!
          }
          .toList()
        if (entities.isNotEmpty()) {
          persistService.batchInsertOrUpdate(entities)
        }
          """.trimIndent()
        )
        .endControlFlow()
        .build()
    )
  }

  private fun createExpirationTask() {
    cache.addFunction(
      FunSpec.builder("createExpirationTask")
        .addModifiers(KModifier.PRIVATE)
        .returns(Function_Unit)
        .beginControlFlow("return")
        .addCode(
          """
            val accessTimeThreshold = currentMillis() - conf.expireAfterAccess.toMillis()
            cache.values.asSequence()
              .filter { 
                it.accessTime <= accessTimeThreshold && (!it.hasEntity() || expireEvaluator.canExpire(it.getEntitySilently()!!)) 
              }
              .forEach {
                cache.computeIfPresent(it.getId()) { _, v ->
                  if (v.accessTime > accessTimeThreshold) {
                    v
                  } else {
                    v.setExpired()
                    if (v.hasEntity()) {
                      writeToDB(v.getEntitySilently()!!)
                    }
                    null
                  }
                }
              }
          """.trimIndent()
        )
        .endControlFlow()
        .build()
    )
  }

  private fun writeToDB() {
    cache.addFunction(
      FunSpec.builder("writeToDB")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("e", E_Entity_ID)
        .addCode(
          """
              val id = e.%L
              persistingEntities[id] = e
              persistService.insertOrUpdate(e).onComplete { result ->
                if (result.isSuccess) {
                  persistingEntities.remove(id)
                } else {
                  // restore into cache
                  cache.compute(id) { k, v ->
                    val persisting = persistingEntities.remove(k)
                    when {
                      persisting == null -> v
                      v == null -> CacheObj(persisting, currentMillis())
                      else -> { // v !=null && persisting != null
                        val cacheEntity = v.getEntitySilently()
                        if (cacheEntity != null && cacheEntity !== persisting) { // should be the same
                          logger.error { 
                            %S 
                          }
                        }
                        v
                      }
                    }
                  }
                  logger.error(result.%M()) { "持久化失败: ${'$'}{entityClass.simpleName}(${'$'}id)" }
                }
              }
          """.trimIndent(),
          getId(),
          """
            缓存中对象与持久化队列中的对象不一致:
            cache: ${'$'}{Json.stringify(cacheEntity)}
            queue: ${'$'}{Json.stringify(persisting)}
          """.trimIndent(),
          getCause
        )
        .build()
    )
  }

  private fun delete() {
    cache.addFunction(
      FunSpec.builder("delete")
        .addModifiers(KModifier.PRIVATE)
        .returns(Boolean::class)
        .addParameter("id", getIdType())
        .beginControlFlow("if (deleted == null)")
        .beginControlFlow("synchronized(this)")
        .beginControlFlow("if (deleted == null)")
        .addStatement("deleted = %T()", getDeletedSetType())
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .addStatement("return deleted!!.add(id)")
        .build()
    )
  }

  private fun isDeleted() {
    cache.addFunction(
      FunSpec.builder("isDeleted")
        .addModifiers(KModifier.PRIVATE)
        .returns(Boolean::class)
        .addParameter("id", getIdType())
        .addStatement("return deleted?.contains(id) ?: false")
        .build()
    )
  }

  private fun requireNotDeleted() {
    cache.addFunction(
      FunSpec.builder("requireNotDeleted")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("id", getIdType())
        .addStatement("""if (isDeleted(id)) throw IllegalStateException("实体已被删除: ${'$'}{entityClass.simpleName}(${'$'}id)")""")
        .build()
    )
  }

  private fun computeIfAbsent1() {
    cache.addFunction(
      FunSpec.builder("computeIfAbsent")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("id", getIdType())
        .addParameter("loader", getLoaderType().copy(true))
        .addStatement("return computeIfAbsent(id, false, loader)")
        .build()
    )
  }

  private fun computeIfAbsent2() {
    cache.addFunction(
      FunSpec.builder("computeIfAbsent")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("id", getIdType())
        .addParameter("createIfAbsent", Boolean::class)
        .addParameter("loader", getLoaderType().copy(true))
        .returns(E_Entity_ID.copy(true))
        .addCode(
          CodeBlock.builder()
            .addStatement("var cacheObj = cache[id]")
            .beginControlFlow("if (cacheObj != null)")
            .beginControlFlow("if (cacheObj.isEmpty() && !createIfAbsent)")
            .addStatement("return null")
            .endControlFlow()
            .addStatement("val entity = cacheObj.getEntity()")
            .beginControlFlow("if (entity != null && !cacheObj.isExpired())")
            .addStatement("return entity")
            .endControlFlow()
            .endControlFlow()
            .addStatement("if (loader === null) return null")
            .addStatement("cacheObj = cache.compute(id) { k, obj ->")
            .indent()
            .beginControlFlow("if (isDeleted(k))")
            .addStatement("null")
            .nextControlFlow("else if (obj == null || obj.isEmpty())")
            .addStatement("val v = loader(k)")
            .addStatement("// if (v == null) CacheObj.empty() else CacheObj(v, currentMillis())")
            .addStatement("CacheObj(v, currentMillis())")
            .nextControlFlow("else")
            .addStatement("obj.accessTime = currentMillis()")
            .addStatement("obj")
            .endControlFlow()
            .unindent()
            .addStatement("}")
            .addStatement("return cacheObj?.getEntitySilently()")
            .build()
        )
        .build()
    )
  }

  private fun entitySequence() {
    cache.addFunction(
      FunSpec.builder("entitySequence")
        .addModifiers(KModifier.PRIVATE)
        .addStatement("return cache.values.asSequence().map { it.getEntitySilently() }.filterNotNull() + persistingEntities.values.asSequence()")
        .build()
    )
  }
}
