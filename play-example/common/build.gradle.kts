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

  api("org.springframework.boot:spring-boot-starter-webflux:2.5.7") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }

  api("org.springframework.boot:spring-boot-starter-actuator:2.5.7") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }

  api("com.alibaba.rsocket:alibaba-rsocket-core:1.1.2")

  api("com.alibaba.rsocket:alibaba-rsocket-spring-boot-starter:1.1.2") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }
  implementation(Deps.ByteBuddy)

  api("org.springframework:spring-messaging:5.3.13")
}
