package play.codegen.ksp.resource

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 *
 * @author LiangZengle
 */
@AutoService(SymbolProcessorProvider::class)
class ResourceSetProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ResourceSetGenerator(environment)
  }
}
