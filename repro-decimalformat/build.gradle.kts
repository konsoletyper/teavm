plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

repositories { mavenCentral() }

val selectedClasslib = providers.gradleProperty("classlibJar")
    .map { file(it) }
    .orElse(file("../classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar"))
    .get()

if (!selectedClasslib.isFile) {
    throw GradleException("Selected classlib JAR does not exist: $selectedClasslib")
}

dependencies {
    implementation(files(selectedClasslib))
    teavm(teavm.libs.jsoApis)
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

teavm.js {
    mainClass = "repro.DecimalFormatContract"
    obfuscated = false
    strict = true
}

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val publishedClasslibName = "teavm-classlib-0.16.0.jar"
    val targetClasspath = objects.fileCollection().from(classpath.from.toList())
        .filter { it.name != publishedClasslibName }
    classpath.setFrom(targetClasspath, selectedClasslib)

    val originalDaemonClasspath = objects.fileCollection().from(daemonClasspath.from.toList())
    daemonClasspath.setFrom(originalDaemonClasspath,
        targetClasspath.filter { it.name == "teavm-platform-0.16.0.jar" })
    outOfProcess.set(true)
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.DecimalFormatContract")
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
