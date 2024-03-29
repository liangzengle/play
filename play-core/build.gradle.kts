dependencies {
  api(project(":play-httpclient-api"))
  api(project(":play-eclipse-collectionx"))
  api(Deps.Asm)
  api(Deps.Guava)
  api(Deps.Jctools)
  api(Deps.KotlinLogging)
  api(Deps.Slf4j.Api)
  api(Deps.Jackson.Kotlin)
  api(Deps.Jackson.Jdk8)
  api(Deps.Jackson.JavaTime)
  api(Deps.Jackson.Guava)
  api(Deps.Jackson.EclipseCollections)
  api(Deps.TsConfig)
  api(Deps.EL.Api)
  api(Deps.EL.Impl)
  api(Deps.EL.Mvel)
  api(Deps.ClassGraph)
  api(Deps.Caffeine)
  api(Deps.Micrometer)
  api(Deps.UnsafeAccessor)
  ksp(Deps.AutoServiceKsp)
  compileOnly(Deps.Kotlinx.Serialization.Protobuf)
  compileOnly(Deps.Wire.Runtime)
  api(Deps.Reactor.Core)
  compileOnly(Deps.Netty.Buffer)

  testImplementation(Deps.Netty.Buffer)
}
