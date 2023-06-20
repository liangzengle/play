dependencies {
  api(project(":play-db"))
  api(libs.monogo.driver.rx)
  api(libs.netty.handler)
  api(libs.netty.buffer)
  api(libs.netty.epoll)
  implementation(libs.jackson.bson)
}
