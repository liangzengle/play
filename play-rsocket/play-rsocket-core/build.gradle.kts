dependencies {
  api(project(":play-rsocket-common"))
  api(project(":play-kryo"))
  api(Deps.Slf4j.Api)
  api(Deps.RSocket.Core)
  api(Deps.RSocket.Transport.Netty)

  implementation(Deps.ByteBuddy)
  implementation(kotlin("reflect"))

  testImplementation(Deps.Kryo)
  testImplementation(Deps.Assertj.Core)
}
