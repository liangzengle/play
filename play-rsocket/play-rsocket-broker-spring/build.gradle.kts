dependencies {
  api(project(":play-rsocket-broker"))
  api(project(":play-rsocket-core-spring"))
  compileOnly(Deps.SpringBoot.Starter)
}
