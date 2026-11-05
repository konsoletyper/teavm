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

teavm.js { mainClass = "repro.ConcurrencyContract" }

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val sourceBuiltClasslib = file(providers.gradleProperty("classlibJar").getOrElse(
        "build/inputs/classlib-0.16.0-SNAPSHOT-original.jar"))
    require(sourceBuiltClasslib.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    val originalTargetEntries = classpath.from.toList()
    val targetClasspath = objects.fileCollection().from(originalTargetEntries)
        .filter { it.name != "teavm-classlib-0.16.0.jar" }
    classpath.setFrom(targetClasspath, sourceBuiltClasslib)

    val platformRuntime = targetClasspath.filter { it.name == "teavm-platform-0.16.0.jar" }
    daemonClasspath.from(platformRuntime)
    outOfProcess.set(true)
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.ConcurrencyContract")
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
