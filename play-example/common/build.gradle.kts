dependencies {
  api(project(":play-akka"))
  api(project(":play-res"))
  api(project(":play-net"))

  api(Deps.Log4j.Core)
  api(Deps.Log4j.Api)
  api(Deps.Log4j.Jul)
  api(Deps.Log4j.Slf4j)

  api(Deps.SpringBoot.Starter) {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }
}
