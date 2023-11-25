dependencies {
  implementation(project(":play-util"))

  api(libs.netty.handler)
  api(libs.netty.http)
  api(libs.netty.epoll)
}
