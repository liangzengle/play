plugins {
  kotlin("plugin.serialization") version Versions.Kotlin
}
dependencies {
  api(project(":play-core"))
  compileOnly(project(":play-codec"))
  compileOnly(Deps.Kotlinx.Serialization.Protobuf)
}
