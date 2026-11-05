plugins {
    java
    checkstyle
    id("org.teavm") version "0.15.0"
}
repositories { mavenCentral() }
dependencies {
    implementation(files("../classlib/build/libs/classlib-0.15.0-SNAPSHOT-original.jar"))
    teavm(teavm.libs.jsoApis)
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
sourceSets.main {
    java.srcDir("../classlib/src/main/java")
    java.include("repro/**")
}
checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}
teavm.js { mainClass = "repro.TimeUnitContract" }
tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val sourceBuiltClasslib = file("../classlib/build/libs/classlib-0.15.0-SNAPSHOT-original.jar")
    val originalClasspathEntries = classpath.from.toList()
    val unpatchedTargetClasspath = objects.fileCollection().from(originalClasspathEntries)
        .filter { it.name != "teavm-classlib-0.15.0.jar" }
    classpath.setFrom(unpatchedTargetClasspath, sourceBuiltClasslib)

    val originalDaemonClasspathEntries = daemonClasspath.from.toList()
    val unpatchedDaemonClasspath = objects.fileCollection().from(originalDaemonClasspathEntries)
    val platformRuntime = unpatchedTargetClasspath.filter { it.name == "teavm-platform-0.15.0.jar" }
    daemonClasspath.setFrom(unpatchedDaemonClasspath, platformRuntime)
    outOfProcess.set(true)
}
tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.TimeUnitContract")
}
tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
