plugins {
  id("com.squareup.wire") version libs.versions.wire.get()
}

dependencies {
  api(libs.wire.runtime)
  implementation(libs.wire.schema)
  implementation(libs.wire.compiler)
  implementation(libs.wire.grpc.client)
  implementation(libs.wire.grpc.server)
  implementation(libs.kotlinpoet.asProvider())
  api(libs.eclipse.collections.api)
  api(project(":play-eclipse-collectionx"))

  testImplementation(libs.wire.schema.tests)
}

sourceSets.main {
  kotlin.srcDir("build/generated/source/wire")
}

tasks.getByName("compileJava").enabled = false

wire {
  sourcePath {
    srcDir("src/main/resources/play")
//    srcDir("src/main/resources/play/none")
  }
  kotlin {}
}
