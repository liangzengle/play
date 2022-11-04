package play.plugin.modularcode

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

open class ModularCodeGradleExtension {
  var enabled = true
  var annotation: String = ""
}

@AutoService(KotlinCompilerPluginSupportPlugin::class)
class ModularCodeGradlePlugin : KotlinCompilerPluginSupportPlugin {
  override fun apply(target: Project): Unit = with(target) {
    extensions.create("modularCode", ModularCodeGradleExtension::class.java)
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(ModularCodeGradleExtension::class.java)
    return project.provider {
      listOf(
        SubpluginOption(key = "annotation", value = extension.annotation),
        SubpluginOption(key = "enabled", value = extension.enabled.toString())
      )
    }
  }

  override fun getCompilerPluginId(): String {
    return ModularCodeCommandLingProcessor.PLUGIN_ID
  }

  override fun getPluginArtifact(): SubpluginArtifact {
    return SubpluginArtifact("play", "play-modular-code", "0.1")
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return true
  }
}
