dependencies {
  api(project(":play-akka"))
  api(Deps.Akka.Serialization.Jackson)
  api(Deps.Akka.Serialization.Kryo)

  api(project(":play-res"))
  api(project(":play-net"))
  api(project(":play-httpclient-ktor"))
  api(project(":play-rsocket-client-spring"))

  api(Deps.SpringBoot.Starter) {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }
  api(Deps.SpringBoot.StarterLog4j2)
}
