plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}
repositories { mavenCentral() }
dependencies { teavm(teavm.libs.jsoApis); compileOnly("org.teavm:teavm-core:0.16.0") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}
teavm.js {
    mainClass = "repro.RuntimeApiContract"
    obfuscated = false
    strict = true
}
tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath + files("duplicates")
    mainClass.set("repro.RuntimeApiContract")
}
tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkJar = file(providers.gradleProperty("classlibJar").getOrElse(
        "../classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar"))
    require(forkJar.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val target = originalTarget.filter { it.name != "teavm-classlib-0.16.0.jar" }
    classpath.setFrom(target, forkJar, files("duplicates"))
    daemonClasspath.from(target.filter { it.name == "teavm-platform-0.16.0.jar" })
    providers.gradleProperty("coreJar").orNull?.let { path ->
        val forkCore = file(path)
        require(forkCore.isFile) { "Build :core:relocateJar before supplying -PcoreJar" }
        val originalDaemon = objects.fileCollection().from(daemonClasspath.from.toList())
        daemonClasspath.setFrom(originalDaemon.filter { it.name != "teavm-core-0.16.0.jar" }, forkCore)
    }
    outOfProcess.set(true)
}
tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}

