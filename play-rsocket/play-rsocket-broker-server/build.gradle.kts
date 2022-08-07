import java.io.Writer

plugins {
  application
}

application {
  mainClass.set("play.rsocket.broker.BrokerAppKt")
  applicationDistribution.into("conf") { from("src/main/resources") }
}

tasks.withType<CreateStartScripts> {
  applicationName = "app"
}

tasks.startScripts {
  unixStartScriptGenerator = PlayUnixStartScriptGenerator()
  windowsStartScriptGenerator = PlayWindowsStartScriptGenerator()
}

dependencies {
  implementation(project(":play-rsocket-broker-spring"))
  implementation(Deps.SpringBoot.Starter) {
    exclude("org.springframework.boot", module = "spring-boot-starter-logging")
  }
  implementation(Deps.SpringBoot.StarterLog4j2)
}


class DelegatinJavaAppStartScriptGenerationDetails(
  delegate: JavaAppStartScriptGenerationDetails
) : JavaAppStartScriptGenerationDetails by delegate {
  override fun getClasspath(): MutableList<String> {
    return arrayListOf("lib/*", "conf")
  }
}

class PlayUnixStartScriptGenerator : org.gradle.api.internal.plugins.UnixStartScriptGenerator() {
  override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
    super.generateScript(DelegatinJavaAppStartScriptGenerationDetails(details), destination)
  }
}

class PlayWindowsStartScriptGenerator : org.gradle.api.internal.plugins.WindowsStartScriptGenerator() {
  override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
    super.generateScript(DelegatinJavaAppStartScriptGenerationDetails(details), destination)
  }
}
