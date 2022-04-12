dependencies {
  api(Deps.Rxjava3)

  api(Deps.SpringBoot.StarterWebflux)
  api(Deps.SpringBoot.StarterActuator)
  api(Deps.Brave)
  api(Deps.AlibabaRsocket.Core)
  api(Deps.AlibabaRsocket.Client)
}

configurations {
  all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
  }
}
