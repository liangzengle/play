package plugin

import org.gradle.jvm.application.scripts.JavaAppStartScriptGenerationDetails
import java.io.Writer

class PlayWindowsStartScriptGenerator : org.gradle.api.internal.plugins.WindowsStartScriptGenerator() {
  override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
    super.generateScript(DelegatinJavaAppStartScriptGenerationDetails(details), destination)
  }
}
