dependencies {
  implementation(project(":play-core"))

  api(Deps.Netty.Handler)
  api(Deps.Netty.CodecHttp)
  api(Deps.Netty.Epoll)
  api(Deps.AsyncHttpClient) {
    exclude(group = "io.netty")
  }
}
