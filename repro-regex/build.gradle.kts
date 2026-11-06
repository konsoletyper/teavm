plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

repositories { mavenCentral() }

dependencies {
    teavm(teavm.libs.jsoApis)
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}

teavm.js { mainClass = "repro.MatcherReplacementContract" }

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkClasslib = file(providers.gradleProperty("classlibJar").getOrElse("build/inputs/classlib.jar"))
    val forkCore = file(providers.gradleProperty("coreJar").getOrElse("build/inputs/core.jar"))
    require(forkClasslib.isFile) { "Copy the source-built classlib into build/inputs or pass -PclasslibJar=<jar>" }
    require(forkCore.isFile) { "Copy the paired source-built core into build/inputs or pass -PcoreJar=<jar>" }
    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val target = originalTarget.filter {
        it.name != "teavm-classlib-0.16.0.jar" && it.name != "teavm-core-0.16.0.jar" &&
            it != forkClasslib && it != forkCore
    }
    classpath.setFrom(target, forkClasslib, forkCore)

    val originalDaemon = objects.fileCollection().from(daemonClasspath.from.toList())
    val platformRuntime = target.filter { it.name == "teavm-platform-0.16.0.jar" }
    daemonClasspath.setFrom(originalDaemon.filter { it.name != "teavm-core-0.16.0.jar" }, forkCore, platformRuntime)
    outOfProcess.set(true)
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.MatcherReplacementContract")
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
