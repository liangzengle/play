package play.codegen.ksp.controller

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 *
 * @author LiangZengle
 */
@AutoService(SymbolProcessorProvider::class)
class ControllerProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ControllerProcessor(environment)
  }
}
