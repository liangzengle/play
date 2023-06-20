plugins {
  application
}

tasks.jar {
  val resourceMain = "$buildDir/resources/main"
  val metaInf = "$buildDir/resources/main/META-INF"
  exclude { file ->
    val path = file.file.toPath()
    path.startsWith(resourceMain) && !path.startsWith(metaInf)
  }
}

application {
  mainClass.set("play.rsocket.broker.BrokerAppKt")
  applicationDistribution.into("conf") { from("src/main/resources") }
}

tasks.withType<CreateStartScripts> {
  applicationName = "RSocketBroker"
}

tasks.startScripts {
  unixStartScriptGenerator = plugin.PlayUnixStartScriptGenerator()
  windowsStartScriptGenerator = plugin.PlayWindowsStartScriptGenerator()
}

dependencies {
  implementation(project(":play-rsocket-broker-spring"))
  implementation(libs.springboot.starter.asProvider()) {
    exclude("org.springframework.boot", module = "spring-boot-starter-logging")
  }
  implementation(libs.springboot.starter.log4j2)
}
