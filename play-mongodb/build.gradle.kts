dependencies {
  api(project(":play-db"))
  api(Deps.MongoDB.Driver)
  api(Deps.Netty.Handler)
  api(Deps.Netty.CodecHttp)
  api(Deps.Netty.Epoll)
  implementation(Deps.Jackson.Bson)
}
