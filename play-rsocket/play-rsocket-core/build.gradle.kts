dependencies {
  api(project(":play-rsocket-common"))
  api(project(":play-kryo"))
  api(libs.slf4j.api)
  api(libs.reactor)
  api(libs.rsocket.transport.netty)

  implementation(libs.bytebuddy)
  implementation(libs.kotlin.reflect)

  testImplementation(libs.kryo)
  testImplementation(libs.assertj.core)
}
