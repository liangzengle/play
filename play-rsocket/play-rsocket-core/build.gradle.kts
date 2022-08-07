dependencies {
  api(project(":play-rsocket-common"))
  api(Deps.Slf4j.Api)
  api(Deps.RSocket.Core)
  api(Deps.RSocket.Transport.Netty)
  api(Deps.Kryo)

  implementation(Deps.ByteBuddy)
  implementation(kotlin("reflect"))

  testImplementation(Deps.Kryo)
  testImplementation(Deps.Assertj.Core)
}
