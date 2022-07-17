dependencies {
  api(project(":play-httpclient-api"))
  api(Deps.AsyncHttpClient)
  testImplementation(Deps.Slf4j.Simple)
}
