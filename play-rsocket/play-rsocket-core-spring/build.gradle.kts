dependencies {
  api(project(":play-rsocket-core"))
  compileOnly(libs.springboot.starter.asProvider())
}
