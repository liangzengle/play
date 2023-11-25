package play.codegen.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

abstract class AbstractSymbolProcessor(
  protected val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

  private var _resolver: Resolver? = null
  protected val resolver: Resolver
    get() = _resolver ?: throw IllegalStateException("resolver is null")

  protected val codeGenerator get() = environment.codeGenerator
  protected val logger get() = environment.logger
  protected val options get() = environment.options

  protected fun getOption(key: String): String = options[key] ?: throw NoSuchElementException("no such option: $key")

  protected fun getOptionOrNull(key: String): String? = options[key]

  final override fun process(resolver: Resolver): List<KSAnnotated> {
    _resolver = resolver
    return process()
  }

  abstract fun process(): List<KSAnnotated>

  protected fun write(fileSpec: FileSpec) {
    try {
      fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    } catch (e: Exception) {
      throw IllegalStateException("Failed to write file: ${fileSpec.name}", e)
    }
  }

  protected fun write(typeSpec: TypeSpec, pkg: String) {
    val fileSpec = FileSpec.builder(pkg, typeSpec.name!!).addType(typeSpec).build()
    write(fileSpec)
  }

  protected fun write(type: QualifiedTypeSpec) {
    val pkg = type.getPackage()
    val typeSpec = type.getTypeSpec()
    val fileSpec = FileSpec.builder(pkg, typeSpec.name!!).addType(typeSpec).build()
    write(fileSpec)
  }
}

