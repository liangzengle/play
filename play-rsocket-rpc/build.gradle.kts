dependencies {
  api(project(":play-core"))
  compileOnly(Deps.SpringBoot.Starter)
  compileOnly(Deps.AlibabaRsocket.Client)
  compileOnly(Deps.ByteBuddy)
}
