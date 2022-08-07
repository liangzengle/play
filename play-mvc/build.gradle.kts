plugins {
  kotlin("plugin.serialization") version Versions.Kotlin
}

dependencies {
  api(project(":play-core"))
  compileOnly(Deps.Kotlinx.Serialization.Protobuf)
}
