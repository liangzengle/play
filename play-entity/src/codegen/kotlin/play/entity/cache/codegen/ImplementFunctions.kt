package play.entity.cache.codegen

import com.squareup.kotlinpoet.*
import play.entity.cache.*

/**
 *
 * @author LiangZengle
 */
object ImplementFunctions : EntityCacheComponent() {
  override fun apply() {
    val idType = getIdType()
    cache
      .addFunction(
        FunSpec.builder("invoke")
          .addParameter("id", idType)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return getOrThrow(id)")
          .build()
      )
      .addFunction(
        FunSpec.builder("get")
          .addParameter("id", idType)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return getOrNull(id).%M()", toOptional)
          .build()
      )
      .addFunction(
        FunSpec.builder("getOrNull")
          .addParameter("id", idType)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return computeIfAbsent(id, dbLoader)")
          .addAnnotation(Nullable)
          .build()
      )
      .addFunction(
        FunSpec.builder("getOrThrow")
          .addParameter("id", idType)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("""return getOrNull(id) ?: throw NoSuchElementException("${'$'}{entityClass.simpleName}(${'$'}id)")""")
          .addAnnotation(Nullable)
          .build()
      )
      .addFunctions(
        getOrCreate(idType)
      )
      .addFunction(
        FunSpec.builder("getCached")
          .addParameter("id", idType)
          .addModifiers(KModifier.OVERRIDE)
          .addAnnotation(
            AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()
          )
          .addStatement("return computeIfAbsent(id, null).toOptional()")
          .build()
      )
      .addFunction(
        FunSpec.builder("asSequence")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return cache.values.asSequence().map { it.getEntitySilently() }.filterNotNull()")
          .build()
      )
      .addFunction(
        FunSpec.builder("asStream")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement(
            "return cache.%L.map { it.getEntitySilently() }.%M()",
            if (isPrimitiveId()) "valuesStream()" else "values.stream()",
            filterNotNull
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("create")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("e", E_Entity_ID)
          .returns(E_Entity_ID)
          .addStatement("requireNotDeleted(e.%L)", getId())
          .addStatement("val that = getOrCreate(e.%L) { e }", getId())
          .beginControlFlow("if (e !== that)")
          .addStatement("throw %T(e.javaClass, e.%L)", EntityExistsException, getId())
          .endControlFlow()
          .addStatement("return e")
          .build()
      )
      .addFunction(
        FunSpec.builder("delete")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("e", E_Entity_ID)
          .addStatement("deleteById(e.%L)", getId())
          .build()
      )
      .addFunction(
        FunSpec.builder("deleteById")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("id", idType)
          .addCode(
            CodeBlock.builder()
              .addStatement("cache.compute(id) { k, _ ->")
              .indent()
              .beginControlFlow("if (delete(k))")
              .beginControlFlow("persistService.deleteById(k, entityClass).onFailure")
              .addStatement("""logger.error(it) { "${'$'}{entityClass.simpleName}(${'$'}id)删除失败" }""")
              .endControlFlow()
              .endControlFlow()
              .addStatement("null")
              .unindent()
              .addStatement("}")
              .build()
          )
          .build()
      )
      .addFunction(
        FunSpec.builder("flush")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("id", idType)
          .addStatement("requireNotDeleted(id)")
          .addStatement("val opt = getCached(id)")
          .beginControlFlow("if (!opt.isPresent)")
          .addStatement(
            """throw IllegalStateException("${'$'}{entityClass.simpleName}(${'$'}id)保存失败，与缓存中的对象不一致")"""
          )
          .nextControlFlow("else")
          .addStatement("persistService.update(opt.get())")
          .endControlFlow()
          .build()
      )
      .addFunction(
        FunSpec.builder("size").addModifiers(KModifier.OVERRIDE).addStatement("return cache.size").build()
      )
      .addFunction(
        FunSpec.builder("isCached")
          .addModifiers(KModifier.OVERRIDE)
          .addParameter("id", idType)
          .addStatement("return cache.containsKey(id)").build()
      )
      .addFunction(
        FunSpec.builder("isEmpty").addModifiers(KModifier.OVERRIDE).addStatement("return cache.isEmpty()").build()
      )
      .addFunction(
        FunSpec.builder("isNotEmpty").addModifiers(KModifier.OVERRIDE).addStatement("return !isEmpty()").build()
      )
      .addFunction(
        FunSpec.builder("flush")
          .addModifiers(KModifier.OVERRIDE)
          .addAnnotation(
            AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()
          )
          .addStatement("return persistService.batchInsertOrUpdate(entitySequence().toList()) as %T<Unit>", Future)
          .build()
      )
      .addFunction(
        FunSpec.builder("dump")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return Json.stringify(entitySequence().toList())")
          .build()
      )
  }

  private fun getOrCreate(idType: TypeName): MutableList<FunSpec> {
    val f1 = FunSpec.builder("getOrCreate")
      .addParameter("id", idType)
      .addParameter("creation", getCreationType())
      .addModifiers(KModifier.OVERRIDE)
      .returns(E_Entity_ID)
      .addCode(
        CodeBlock.builder()
          .addStatement("requireNotDeleted(id)")
          .addStatement("return computeIfAbsent(id, true) {")
          .indent()
          .addStatement("var entity = dbLoader(it)")
          .beginControlFlow("if (entity == null)")
          .addStatement("entity = creation(it)")
          .addStatement("initializer.initialize(entity)")
          .addStatement("persistService.insert(entity).onFailure { e ->")
          .indent()
          .addStatement(
            """logger.error(e) { "数据插入失败: ${'$'}{entityClass.simpleName}${'$'}{%T.stringify(entity)}" }""",
            Json
          )
          .addStatement("}")
          .unindent()
          .endControlFlow()
          .addStatement("entity")
          .unindent()
          .addStatement("}!!")
          .build()
      )
      .build()
    val functions = mutableListOf<FunSpec>()
    functions.add(f1)
    val f2 = when (idType) {
      INT -> {
        FunSpec.builder("getOrCreate")
          .addParameter("id", idType)
          .addParameter("creation", Function_Int_E)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return getOrCreate(id, IntToObjFunction { creation(it) })")
          .build()
      }
      LONG -> {
        FunSpec.builder("getOrCreate")
          .addParameter("id", idType)
          .addParameter("creation", Function_Long_E)
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return getOrCreate(id, LongToObjFunction { creation(it) })")
          .build()
      }
      else -> null
    }
    if (f2 != null) {
      functions.add(f2)
    }
    return functions
  }

  override fun accept(): Boolean {
    return true
  }
}
