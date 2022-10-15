package play.dokka

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaPlugin
import play.dokka.model.ClassDescriptorList
import java.io.File

/**
 *
 * @author LiangZengle
 */
object Dokka {

  fun generate(projectDir: File): ClassDescriptorList {
    return generate(projectDir, { it }, DefaultTransformer)
  }

  fun <T> generate(
    projectDir: File,
    configurationOverride: (DokkaConfigurationImpl) -> DokkaConfigurationImpl,
    transformer: (List<DModule>) -> T
  ): T {
    return Generator.generate(projectDir, configurationOverride, transformer)
  }

  private object Generator : BaseAbstractTest() {
    fun <T> generate(
      projectDir: File,
      configurationOverride: (DokkaConfigurationImpl) -> DokkaConfigurationImpl,
      transformer: (List<DModule>) -> T
    ): T {
      val cfg = configurationOverride(
        dokkaConfiguration {
          sourceSet {
            sourceRoots = listOf("src/main/kotlin", "src/main/java").map { projectDir.resolve(it).absolutePath }
          }
        }
      )

      val holder = arrayOfNulls<Any>(1)
      testFromData(cfg, pluginOverrides = listOf(object : DokkaPlugin() {})) {
        preMergeDocumentablesTransformationStage = { models ->
          holder[0] = transformer(models)
        }
      }
      @Suppress("UNCHECKED_CAST")
      return holder[0] as T
    }
  }
}
