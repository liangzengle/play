package play.dokka

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import play.dokka.model.ClassDescriptorList
import testApi.testRunner.TestDokkaConfigurationBuilder
import java.io.File

/**
 *
 * @author LiangZengle
 */
object Dokka {

  const val MAIN_JAVA = "src/main/java"

  const val MAIN_KOTLIN = "src/main/kotlin"

  /**
   * What is a projectDir: {projectDir}/src/main/kotlin
   *
   * @param projectDir File
   * @return ClassDescriptorList
   */
  fun generate(vararg projectDir: File): ClassDescriptorList {
    return generate(listOf(*projectDir))
  }

  fun generate(projectDirs: List<File>): ClassDescriptorList {
    return generate(projectDirs) { }
  }

  fun generate(
    projectDirs: List<File>,
    configurationCustomizer: TestDokkaConfigurationBuilder.() -> Unit
  ): ClassDescriptorList {
    return generate(projectDirs, configurationCustomizer, DefaultTransformer)
  }

  fun <T> generate(
    projectDirs: List<File>,
    configurationCustomizer: TestDokkaConfigurationBuilder.() -> Unit,
    transformer: (List<DModule>) -> T
  ): T {
    return Generator.generate(projectDirs, configurationCustomizer, transformer)
  }

  fun defaultSourceRoots(projectDir: File): Sequence<String> {
    return sequenceOf(projectDir.resolve(MAIN_KOTLIN).absolutePath, projectDir.resolve(MAIN_JAVA).absolutePath)
  }

  fun TestDokkaConfigurationBuilder.withDefaultSourceSet(projectDirs: List<File>) {
    sourceSet {
      sourceRoots = projectDirs.asSequence().flatMap(::defaultSourceRoots).toList()
    }
  }

  private object Generator : BaseAbstractTest() {
    fun <T> generate(
      projectDirs: List<File>,
      configurationCustomizer: TestDokkaConfigurationBuilder.() -> Unit,
      transformer: (List<DModule>) -> T
    ): T {
      val cfg = dokkaConfiguration {
        withDefaultSourceSet(projectDirs)
        configurationCustomizer()
      }
      val holder = arrayOfNulls<Any>(1)
      testFromData(cfg, pluginOverrides = listOf(DokkaPluginImpl)) {
        preMergeDocumentablesTransformationStage = { models ->
          holder[0] = transformer(models)
        }
      }
      @Suppress("UNCHECKED_CAST")
      return holder[0] as T
    }
  }

  @OptIn(DokkaPluginApiPreview::class)
  private object DokkaPluginImpl : DokkaPlugin() {
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement {
      return PluginApiPreviewAcknowledgement
    }
  }
}
