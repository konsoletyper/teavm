plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

repositories {
    mavenCentral()
}

val sourceClasslibJar = providers.gradleProperty("classlibJar")

dependencies {
    if (sourceClasslibJar.isPresent) {
        implementation(files(sourceClasslibJar.get()))
    } else {
        implementation("org.teavm:teavm-classlib:0.16.0")
    }
    implementation("org.teavm:teavm-core:0.16.0")
    implementation("org.teavm:teavm-platform:0.16.0")
    implementation("org.teavm:teavm-interop:0.16.0")
    teavm(teavm.libs.jsoApis)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}

teavm.js {
    mainClass = "repro.ScheduledExecutorRepro"
}

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkJar = file(providers.gradleProperty("classlibJar").getOrElse(
        "../classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar"))
    require(forkJar.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val target = originalTarget.filter {
        it.name != "teavm-classlib-0.16.0.jar" && it != forkJar
    }
    classpath.setFrom(target, forkJar)
    daemonClasspath.from(target.filter { it.name == "teavm-platform-0.16.0.jar" })
    outOfProcess.set(true)
}

tasks.register<JavaExec>("runJavaContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "repro.ScheduledExecutorRepro"
}

tasks.register<Exec>("runJavaScriptRepro") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
