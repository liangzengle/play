dependencies {
  api(project(":play-util"))
  ksp(libs.auto.service.ksp)
  compileOnly(libs.kotlinx.serialization.protobuf)
  compileOnly(libs.wire.runtime)
  api(libs.reactor)
  compileOnly(libs.netty.buffer)
  compileOnly(libs.bundles.micrometer)
  implementation("org.apache.commons:commons-csv:1.10.0")
  implementation("com.opencsv:opencsv:5.8")

  testImplementation(libs.netty.buffer)
}
