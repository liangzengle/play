plugins {
  kotlin("plugin.serialization") version Versions.Kotlin
}
dependencies {
    api( project(":play-core"))
    api(Deps.Kotlinx.Serialization.Protobuf)
}
