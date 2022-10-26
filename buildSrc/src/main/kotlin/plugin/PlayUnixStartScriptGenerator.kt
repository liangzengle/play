package plugin

import org.gradle.jvm.application.scripts.JavaAppStartScriptGenerationDetails
import java.io.Writer

class PlayUnixStartScriptGenerator : org.gradle.api.internal.plugins.UnixStartScriptGenerator() {
  override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
    super.generateScript(DelegatinJavaAppStartScriptGenerationDetails(details), destination)
  }
}
