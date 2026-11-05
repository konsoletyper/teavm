plugins {
    java
    checkstyle
    id("org.teavm") version "0.15.0"
}

repositories { mavenCentral() }
dependencies {
    compileOnly("org.teavm:teavm-core:0.15.0")
    teavm(files("../classlib/build/libs/classlib-0.15.0-SNAPSHOT-original.jar"))
    teavm(teavm.libs.jsoApis)
    teavm("org.teavm:teavm-core:0.15.0")
    teavm("org.teavm:teavm-platform:0.15.0")
    teavm("org.teavm:teavm-extension-spi:0.15.0")
    teavm("org.teavm:teavm-jso-impl:0.15.0")
    teavm("org.teavm:teavm-metaprogramming-impl:0.15.0")
    teavm("com.jcraft:jzlib:1.1.3")
    teavm("joda-time:joda-time:2.12.2")
}
configurations.named("teavm") {
    exclude(group = "org.teavm", module = "teavm-classlib")
}
configurations.named("teavmClasspath") {
    exclude(group = "org.teavm", module = "teavm-classlib")
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
teavm.js { mainClass = "repro.JdkApiContract" }
tasks.register<JavaExec>("jvmContract") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("repro.JdkApiContract")
}
tasks.register<Exec>("runJavaScriptContract") {
    dependsOn("generateJavaScript")
    workingDir = projectDir
    commandLine("node", "-e", "const m=require('./build/generated/teavm/js/teavm-jdk-api-repro.js');m.main([])")
}
