plugins {
  `maven-publish`
  `java-gradle-plugin`
}

group = "play"
version = "0.1"

val pluginName = "play-modular-code"
gradlePlugin {
  plugins {
    register(pluginName) {
      id = "play-modular-code"
      implementationClass = "play.plugin.modularcode.ModularCodeGradlePlugin"
    }
  }
}

dependencies {
  implementation(libs.kotlin.gradle.api)
  implementation(libs.kotlin.compiler)
  implementation(libs.kotlin.reflect)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}
