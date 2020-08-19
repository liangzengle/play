package play.plugin.modularcode

import com.google.auto.service.AutoService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PlayModularCodeGradlePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.extensions.create("modularCode", PlayModularCodeGradleExtension::class.java)
  }
}

open class PlayModularCodeGradleExtension {
  var enabled = true
  var annotation = listOf<String>()
}

@AutoService(KotlinGradleSubplugin::class)
class PlayModularCodeGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
  override fun apply(
    project: Project,
    kotlinCompile: AbstractCompile,
    javaCompile: AbstractCompile?,
    variantData: Any?,
    androidProjectHandler: Any?,
    kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
  ): List<SubpluginOption> {
    val extension = project.extensions.findByType(PlayModularCodeGradleExtension::class.java)
      ?: PlayModularCodeGradleExtension()
    val annotationOption = extension.annotation
      .map { SubpluginOption(key = "annotation", value = it) }
    val enabledOption = SubpluginOption(
      key = "enabled", value = extension.enabled.toString()
    )
    return annotationOption + enabledOption
  }

  override fun getCompilerPluginId(): String {
    return PlayModularCodeCommandLingProcessor.PLUGIN_ID
  }

  override fun getPluginArtifact(): SubpluginArtifact {
    return SubpluginArtifact("play", "modular-code", "0.1")
  }

  override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
    return project.plugins.hasPlugin(PlayModularCodeGradlePlugin::class.java)
  }

}


