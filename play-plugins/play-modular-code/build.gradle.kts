plugins {
  `maven-publish`
  `java-gradle-plugin`
}

group = "play"
version = "0.1"

val pluginName = "modularCode"
gradlePlugin {
  plugins {
    register(pluginName) {
      id = "play.modular-code"
      implementationClass = "play.plugin.modularcode.ModularCodeGradlePlugin"
    }
  }
}

dependencies {
  implementation(Deps.Kotlin.GradleApi)
  implementation(Deps.Kotlin.Compiler)
  implementation(Deps.Kotlin.Reflect)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      artifactId = "modular-code"
      from(components["java"])
    }
  }
}
