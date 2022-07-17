dependencies {
  compileOnly(project(":play-core"))
  compileOnly(project(":play-net"))
  compileOnly(project(":play-res"))
  compileOnly(project(":play-entity"))
  compileOnly(project(":play-mongodb"))
  compileOnly(project(":play-akka"))
  compileOnly(project(":play-httpclient-async"))
  compileOnly(project(":play-httpclient-ktor"))
  compileOnly(Deps.SpringBoot.Starter)
}
