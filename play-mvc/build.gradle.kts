plugins {
  kotlin("plugin.serialization") version libs.versions.kotlin.asProvider().get()
}

dependencies {
  api(project(":play-core"))
  compileOnly(libs.kotlinx.serialization.protobuf)
}
