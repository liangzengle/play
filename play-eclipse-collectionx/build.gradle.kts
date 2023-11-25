val generateSourceDir = "${layout.buildDirectory}/generated-sources"

sourceSets {
    create("codegen") {
        kotlin.srcDir("src/codegen/kotlin")
    }
    main {
        kotlin.srcDirs(generateSourceDir)
    }
}

fun DependencyHandler.codegenImplementation(dependencyNotation: Any): Dependency? =
    add("codegenImplementation", dependencyNotation)

dependencies {
    api(libs.eclipse.collections.asProvider())

    codegenImplementation(libs.kotlinpoet.asProvider())
    codegenImplementation(libs.eclipse.collections.asProvider())
}

tasks.register("generateCode", JavaExec::class) {
    description = "Generate code using KotlinPoet"
    mainClass.set("play.eclipse.collectionx.codegen.MainKt")
    classpath = sourceSets["codegen"].runtimeClasspath
    args("${layout.buildDirectory}/generated-sources")

    outputs.upToDateWhen {
        !tasks["compileCodegenKotlin"].didWork || !tasks["compileCodegenJava"].didWork
    }
    outputs.dir(generateSourceDir)
    outputs.cacheIf { true }
}

tasks.compileJava {
    dependsOn("generateCode")
}
tasks.compileKotlin {
    dependsOn("generateCode")
}
