dependencies {
  api(project(":play-rsocket-client"))
  api(project(":play-rsocket-core-spring"))
  compileOnly(Deps.SpringBoot.Starter)
}
