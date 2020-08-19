package play.plugin.modularcode

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import play.plugin.modularcode.ModularCodeConfigurationKeys.KEY_ANNOTATION
import play.plugin.modularcode.ModularCodeConfigurationKeys.KEY_ENABLED

object ModularCodeConfigurationKeys {
  val KEY_ENABLED = CompilerConfigurationKey.create<Boolean>("enabled")
  val KEY_ANNOTATION = CompilerConfigurationKey.create<List<String>>("annotation")
}

@AutoService(CommandLineProcessor::class)
class PlayModularCodeCommandLingProcessor : CommandLineProcessor {
  companion object {
    val PLUGIN_ID = "play.modular-code"
    val ENABLE_OPTION = CliOption("enabled", "<true|false>", "whether plugin is enabled", false)
    val ANNOTATION_OPTION =
      CliOption("annotation", "<fqname>", "Annotation qualified names", true, allowMultipleOccurrences = true)
  }

  override val pluginId: String = PLUGIN_ID
  override val pluginOptions: Collection<AbstractCliOption> = listOf(ENABLE_OPTION, ANNOTATION_OPTION)

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    when (option) {
      ENABLE_OPTION -> configuration.put(KEY_ENABLED, value.toBoolean())
      ANNOTATION_OPTION -> configuration.appendList(KEY_ANNOTATION, value)
      else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
  }
}

