plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}
repositories { mavenCentral() }
dependencies { teavm(teavm.libs.jsoApis) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}
teavm.js {
    mainClass = "repro.SecureRandomContract"
    obfuscated = false
    strict = true
}
tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.SecureRandomContract")
}
tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkJar = file(providers.gradleProperty("classlibJar").getOrElse(
        "../classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar"))
    require(forkJar.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val jsoJar = file(providers.gradleProperty("jsoJar").getOrElse(
        "../jso/apis/build/libs/apis-0.16.0-SNAPSHOT.jar"))
    require(jsoJar.isFile) { "Build :jso:apis:relocateJar first, or pass -PjsoJar=<jar>" }
    val target = originalTarget.filter {
        it.name != "teavm-classlib-0.16.0.jar" && it.name != "teavm-jso-apis-0.16.0.jar"
    }
    classpath.setFrom(target, forkJar, jsoJar)
    daemonClasspath.from(target.filter { it.name == "teavm-platform-0.16.0.jar" })
    outOfProcess.set(true)
}
tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}

tasks.register<Exec>("runUnavailableContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs", "unavailable")
}

tasks.register<Exec>("runRealCryptoContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs", "real")
}

