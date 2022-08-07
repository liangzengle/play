dependencies {
  implementation(project(":play-example-rpc-api"))

  compileOnly(project(":play-codegen-rpc"))
  ksp(project(":play-codegen-rpc"))
  implementation(project(":play-rsocket-client-spring"))

  implementation(Deps.SpringBoot.Starter)
}
