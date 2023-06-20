dependencies {
  api(project(":play-rsocket-client"))
  api(project(":play-rsocket-core-spring"))
  compileOnly(libs.springboot.starter.asProvider())
}
