dependencies {
  implementation(project(":play-rsocket-common"))
  api(Deps.Reactor.Core)
  compileOnly(project(":play-rsocket-core"))
  compileOnly(project(":play-codegen-rpc"))
  ksp(project(":play-codegen-rpc"))
}
