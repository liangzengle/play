dependencies {
  implementation(project(":play-core"))

  api(libs.netty.handler)
  api(libs.netty.http)
  api(libs.netty.epoll)
}
