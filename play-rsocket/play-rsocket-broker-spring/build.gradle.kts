dependencies {
  api(project(":play-rsocket-broker"))
  api(project(":play-rsocket-core-spring"))
  compileOnly(libs.springboot.starter.asProvider())
}
