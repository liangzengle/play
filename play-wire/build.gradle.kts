plugins {
  id("com.squareup.wire") version Versions.Wire
}

dependencies {
  api(Deps.Wire.Runtime)
  implementation(Deps.Wire.Schema)
  implementation(Deps.Wire.Compiler)
  implementation(Deps.Wire.GrpcClient)
  implementation(Deps.Wire.GrpcServerGenerator)
  implementation(Deps.KotlinPoet.Poet)
  api(Deps.Eclipse.Collections.Api)
  api(project(":play-eclipse-collectionx"))

  testImplementation(Deps.Wire.SchameTests)
}

sourceSets.main {
  kotlin.srcDir("build/generated/source/wire")
}

tasks.getByName("compileJava").enabled = false

wire {
  sourcePath {
//    srcDir("src/main/resources/play")
    srcDir("src/main/resources/play/none")
  }
  kotlin {}
}
