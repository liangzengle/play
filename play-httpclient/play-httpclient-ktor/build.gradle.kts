dependencies {
  api(project(":play-httpclient-api"))
  implementation(Deps.Ktor.Client.Core)
  implementation(Deps.Ktor.Client.Cio)
  testImplementation(Deps.Slf4j.Simple)
}
