package plugin

import org.gradle.jvm.application.scripts.JavaAppStartScriptGenerationDetails

class DelegatinJavaAppStartScriptGenerationDetails(
  delegate: JavaAppStartScriptGenerationDetails
) : JavaAppStartScriptGenerationDetails by delegate {
  override fun getClasspath(): MutableList<String> {
    return arrayListOf("lib/*", "conf")
  }
}
