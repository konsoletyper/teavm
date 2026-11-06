plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

dependencies {
    teavm(teavm.libs.jsoApis)
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}

teavm.js { mainClass = providers.gradleProperty("teavmEntry").getOrElse("repro.ThrowableContract") }

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkClasslib = file(providers.gradleProperty("classlibJar").getOrElse(
        "build/inputs/classlib.jar"))
    val forkCore = file(providers.gradleProperty("coreJar").getOrElse(
        "build/inputs/core.jar"))
    require(forkClasslib.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    require(forkCore.isFile) { "Build the fork :core:jar first, or pass -PcoreJar=<jar>" }

    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    classpath.setFrom(originalTarget.filter {
        it.name != "teavm-classlib-0.16.0.jar" && it.name != "teavm-core-0.16.0.jar"
    }, forkClasslib, forkCore)
    val originalDaemon = objects.fileCollection().from(daemonClasspath.from.toList())
    daemonClasspath.setFrom(originalDaemon.filter { it.name != "teavm-core-0.16.0.jar" }, forkCore)
    outOfProcess.set(true)
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.ThrowableContract")
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}

tasks.register<Exec>("runPrintOnlyContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run-print-only.cjs")
}
