plugins {
    java
    checkstyle
    id("org.teavm") version "0.16.0"
}

repositories { mavenCentral() }
dependencies {
    compileOnly("org.teavm:teavm-core:0.16.0")
    teavm(teavm.libs.jsoApis)
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
sourceSets.main {
    java.include("repro/**")
}
checkstyle {
    toolVersion = "10.21.0"
    configDirectory.set(layout.projectDirectory.dir("../config/checkstyle"))
}
teavm.js { mainClass = "repro.JdkApiContract" }
tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val sourceBuiltClasslib = file("../classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar")
    val originalTargetEntries = classpath.from.toList()
    val targetClasspath = objects.fileCollection().from(originalTargetEntries)
        .filter { it.name != "teavm-classlib-0.16.0.jar" }
    classpath.setFrom(targetClasspath, sourceBuiltClasslib)

    daemonClasspath.from(targetClasspath.filter { it.name == "teavm-platform-0.16.0.jar" })
    outOfProcess.set(true)
}
tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.JdkApiContract")
}
tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
