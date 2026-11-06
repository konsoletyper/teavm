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

teavm.js { mainClass = "repro.LocaleContract" }

tasks.named<org.teavm.gradle.tasks.GenerateJavaScriptTask>("generateJavaScript") {
    val forkClasslib = file(providers.gradleProperty("classlibJar").getOrElse(
        "../classlib/build/libs/classlib-0.16.0-SNAPSHOT-original.jar"))
    require(forkClasslib.isFile) { "Build the fork :classlib:jar first, or pass -PclasslibJar=<jar>" }
    val originalTarget = objects.fileCollection().from(classpath.from.toList())
    val target = originalTarget.filter { it.name != "teavm-classlib-0.16.0.jar" }
    classpath.setFrom(target, forkClasslib)
    daemonClasspath.from(target.filter { it.name == "teavm-platform-0.16.0.jar" })
    outOfProcess.set(true)
}

tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.LocaleContract")
}

tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "run.cjs")
}
